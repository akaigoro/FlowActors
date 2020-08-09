package org.df4j.core.dataflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.df4j.core.dataflow.Actor.ActorState.*;

/**
 * {@link Actor} is an {@link Actor} whose {@link Actor#runAction()} method can be executed repeatedly,
 * if its input ports receives more input arguments.
 * In other words, Actor is a repeatable asynchronous procedure.
 *  `Actors` here are <a href="https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf"><i>dataflow actors whith arbitrary number of parameters.</i></a>
 *  An Actor as designed by Carl Hewitt is just an {@link Actor} with single input port, and is implemented as {@link ClassicActor}.
 */
public abstract class Actor {
    public static final int PORTS_ALL  = 0xFFFFFFFF;
    public static final int PORTS_NONE = 0x00000001;
    public static final int MAX_PORT_NUM = 31;
    private static final boolean checkingMode = true; // todo false
    protected ActorState state = Created;
    protected Throwable completionException;
    protected boolean completed;
    private int activePortsScale = PORTS_ALL;
    /**
     * blocked initially, until {@link #start} called.
     */
    private ArrayList<Port> ports = new ArrayList<>(4);
    private int blockedPortsScale = 0;
    private ControlPort controlport = new ControlPort(this);
    private ExecutorService executor;

    {
        setActivePorts(PORTS_ALL);
    }

    protected void setActivePorts(int scale) {
        activePortsScale = PORTS_NONE | scale;
    }

    protected int setUnBlocked(int portNum) {
        return (blockedPortsScale &= ~(1 << portNum)) & activePortsScale;
    }

    protected void run() {
        try {
            runAction();
            synchronized (this) {
                switch (state) {
                    case Completed:
                    return;
                default:
                    _controlportUnblock();
                }
            }
        } catch (Throwable e) {
            completeExceptionally(e);
        }
    }

    public ActorState getState() {
        return state;
    }

    private void setBlocked(int portNum) {
        blockedPortsScale |= (1<<portNum);
    }

    protected boolean isBlocked(int portNum) {
        return (blockedPortsScale & (1<<portNum)) != 0;
    }

    /**
     * moves this {@link Actor} from {@link ActorState#Created} state to {@link ActorState#Running}
     * (or {@link ActorState#Suspended}, if was suspended in constructor).
     *
     * In other words, passes the control token to this {@link Actor}.
     * This token is consumed when this block is submitted to an executor.
     * Only the first call works, subsequent calls are ignored.
     */
    public synchronized void start() {
        if (state != Created) {
            return;
        }
        _controlportUnblock();
    }

    /**
     * finishes parent activity normally.
     */
    public void complete() {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            state = Completed;
        }
        _complete(null);
    }

    /**
     * finishes parent activity exceptionally.
     * @param ex the exception
     */
    public void completeExceptionally(Throwable ex) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            state = Completed;
        }
        if (ex == null) {
            throw new IllegalArgumentException();
        }
        _complete(ex);
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        getExecutor().execute(this::run);
    }

    public boolean isAlive() {
        return !isCompleted();
    }

    protected void _controlportUnblock() {
        state = Blocked;
        controlport.unblock();
    }

    protected void _controlportBlock() {
        state = Running;
        controlport.block();
    }

    /**     * User's action.
     * User is adviswd top override this method, but overriding {@link #fire()} is also possible
     *
     * @throws Throwable when thrown, this node is considered failed.
     */
    protected abstract void runAction() throws Throwable;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());sb.append(" ");
        Throwable completionException1 = this.completionException;
        if (!completed) {
            sb.append("not completed");
        } else if (completionException1 == null) {
            sb.append("completed successfully");
        } else {
            sb.append("completed with exception: ");
            sb.append(completionException1.toString());
        }
        return sb.toString() + "/"+state;
    }

    public void setExecutor(ExecutorService executor) {
        synchronized(this) {
            this.executor = executor;
        }
    }

    public void setExecutor(Executor executor) {
        ExecutorService service = new AbstractExecutorService(){
            @Override
            public void execute(Runnable command) {
                executor.execute(command);
            }

            @Override
            public void shutdown() {

            }

            @Override
            public List<Runnable> shutdownNow() {
                return null;
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return false;
            }
        };
        setExecutor(service);
    }

    public ExecutorService getExecutor() {
        synchronized(this) {
            if (executor == null) {
                Thread currentThread = Thread.currentThread();
                if (currentThread instanceof ForkJoinWorkerThread) {
                    executor = ((ForkJoinWorkerThread) currentThread).getPool();
                } else {
                    executor = ForkJoinPool.commonPool();
                }
            }
            return executor;
        }
    }

    public void setCompletionException(Throwable completionException) {
        this.completionException = completionException;
    }

    /**
     * @return completion Exception, if this {@link Completable} was completed exceptionally;
     *         null otherwise
     */
    public synchronized Throwable getCompletionException() {
        return completionException;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * @return true if this {@link Completable} was completed normally or exceptionally;
     *         false otherwise
     */
    public synchronized boolean isCompleted() {
        return completed;
    }

    protected void _complete(Throwable e) {
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            this.completionException = e;
            notifyAll();
        }
    }

    /**
     * waits this {@link Completable} to complete
     * @throws InterruptedException if this thread interrupted
     */
    public void join()  throws InterruptedException  {
        synchronized(this) {
            while (!completed) {
                wait();
            }
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeoutMillis timeout in millisecomds
     * @return true if completed;
     *         false if timout reached
     */
    public synchronized boolean blockingAwait(long timeoutMillis) {
        long targetTime = System.currentTimeMillis()+timeoutMillis;
        try {
            for (;;) {
                if (completed) {
                    if (completionException == null) {
                        return true;
                    } else {
                        throw new CompletionException(completionException);
                    }
                }
                if (timeoutMillis <= 0) {
                    return false;
                }
                wait(timeoutMillis);
                timeoutMillis = targetTime - System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    public synchronized boolean blockingAwait(long timeout, TimeUnit unit) {
        long timeoutMillis = unit.toMillis(timeout);
        return blockingAwait(timeoutMillis);
    }

    /**
     * Basic class for all ports (places for tokens).
     * Has 2 states: ready or blocked.
     * When all ports become unblocked, method {@link Actor#fire()} is called.
     * This is clear analogue to the firing of a Petri Net transition.
     */
    public static class Port {
        protected boolean ready;
        protected final Actor parent;
        protected final int portNum;

        public Port(Actor parent, boolean ready) {
            this.parent = parent;
            synchronized(parent) {
                portNum=parent.ports.size();
                if (portNum > MAX_PORT_NUM) {
                    throw new IllegalStateException("too many ports");
                }
                parent.ports.add(this);
                if (!ready) {
                    parent.setBlocked(portNum);
                }
            }
            this.ready = ready;
        }

        public Port(Actor parent) {
            this(parent, false);
        }

        protected Actor getParent() {
            return parent;
        }

        public boolean isReady() {
            synchronized(parent) {
                return ready;
            }
        }

        /**
         * sets this port to a blocked state.
         */
        public synchronized void block() {
            if (!ready) {
                return;
            }
            ready = false;
            synchronized(parent) {
                if (parent.isCompleted()) {
                    return;
                }
                parent.setBlocked(portNum);
            }
        }

        /**
         * sets this port to unblocked state.
         * If all ports become unblocked,
         * this block is submitted to the executor.
         */
        public synchronized void unblock() {
            if (ready) {
                return;
            }
            ready = true;
            synchronized(parent) {
                if (parent.isCompleted()) {
                    return;
                }
                if (parent.blockedPortsScale == 0) {
                    throw new IllegalStateException("port blocked but blockedPortsScale == 0");
                }
                if (parent.setUnBlocked(portNum) == 0) {
                    parent._controlportBlock();
                    parent.fire();
                }
            }
        }

        @Override
        public String toString() {
            return super.toString() + (ready?": ready":": blocked");
        }
    }

    private static class ControlPort extends Port {
        ControlPort(Actor parent) {
            super(parent);
        }
    }

    /**
     * applicable to {@link AsyncProc} also.
     */
    public enum ActorState {
        /**
         * created but not yet started
         */
        Created,

        /**
         *  started but some ports are blocked
         */
        Blocked,

        /**
         * started and all port are ready
         */
        Running,

        /**
         * completed normally or exceptionally.
         * Will never run again.
         */
        Completed,
    }
}
