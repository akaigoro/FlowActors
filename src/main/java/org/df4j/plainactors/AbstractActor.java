package org.df4j.plainactors;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;

public abstract class AbstractActor {
    private Executor excecutor = ForkJoinPool.commonPool();
    private State state = State.CREATED;
    private int blocked = 0;
    Throwable completionException = null;
    /**
     * blocked initially and when running.
     */
    private Port controlPort = new Port();

    protected AbstractActor() {
        init();
    }

    protected void init() {}

    public synchronized void start() {
        if (state != State.CREATED) {
            throw new IllegalStateException();
        }
        state = State.RUNNING;
        controlPort.unBlock();
    }

    protected synchronized void restart() {
        if (state != State.COMPLETED) {
            controlPort.unBlock();
        }
    }

    private synchronized void incBlockCount() {
        blocked++;
    }

    private synchronized void decBlockCount() {
        blocked--;
        if (blocked == 0) {
            controlPort.block();
            excecutor.execute(this::run);
        }
    }

    public synchronized boolean isCompleted() {
        return state == State.COMPLETED;
    }

    protected synchronized void whenComplete() {
        state = State.COMPLETED;
        notifyAll();
    }

    protected synchronized void whenError(Throwable throwable) {
        completionException = throwable;
        state = State.COMPLETED;
        notifyAll();
    }

    protected abstract void turn() throws Throwable;

    private void run() {
        try {
            turn();
            restart();
        } catch (Throwable throwable) {
            whenError(throwable);
        }
    }

    public synchronized void join(long timeout0) throws InterruptedException, TimeoutException {
        long timeout = timeout0;
        long targetTime = System.currentTimeMillis() + timeout;
        while (state!= State.COMPLETED) {
            if (timeout <= 0) {
                throw new TimeoutException();
            }
            wait(timeout);
            timeout = targetTime - System.currentTimeMillis();
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    public enum State {
        CREATED,
        RUNNING,
        COMPLETED
    }

    class Port {
        private boolean isBlocked = true;

        public Port() {
            incBlockCount();
        }

        public boolean isBlocked() {
            return isBlocked;
        }

        public boolean isEmpty() {
            return isBlocked;
        }

        public boolean isFull() {
            return isBlocked;
        }

        protected synchronized void block() {
            if (isBlocked) {
                return;
            }
            isBlocked = true;
            incBlockCount();
        }

        protected synchronized void unBlock() {
            if (!isBlocked) {
                return;
            }
            isBlocked = false;
            decBlockCount();
        }
    }

    public class AsyncSemaPort extends Port {
        private long permissions = 0;

        public AsyncSemaPort(long permissions) {
            setPermissions(permissions);
        }

        public AsyncSemaPort() {
        }

        public synchronized void setPermissions(long newPermissionsValue) {
            permissions = newPermissionsValue;
            if (permissions > 0) {
                unBlock();
            } else {
                block();
            }
        }

        public synchronized void release(long delta) {
            if (delta <= 0) {
                throw new IllegalArgumentException();
            }
            long newPermissionsValue = permissions + delta;
            if (newPermissionsValue < permissions) {
                permissions = Long.MAX_VALUE;
            } else {
                permissions = newPermissionsValue;
            }
            if (permissions > 0) {
                unBlock();
            }
        }

        public synchronized void aquire(long delta) {
            if (delta <= 0) {
                throw new IllegalArgumentException();
            }
            long newPermissionsValue = permissions-delta;
            if (newPermissionsValue > permissions) {
                permissions = Long.MIN_VALUE;
            } else {
                permissions = newPermissionsValue;
            }
            if (permissions <= 0) {
                block();
            }
        }

        public void aquire() {
            aquire(1);
        }
    }

    public class InPort<T> extends Port implements OutMessagePort<T> {
        private T item;
        protected boolean completeSignalled;
        protected Throwable completionException = null;

        public T current() {
            return item;
        }

        public boolean isCompleted() {
            return item==null && completeSignalled;
        }

        public boolean isCompletedExceptionally() {
            return item==null && completeSignalled && completionException != null;
        }

        public Throwable getCompletionException() {
            return completionException;
        }

        @Override
        public void onNext(T item) {
            if (item == null) {
                throw new NullPointerException();
            }
            if (completeSignalled) {
                return;
            }
            if (this.item != null) {
                throw new IllegalStateException();
            }
            this.item = item;
            unBlock();
        }

        @Override
        public void onError(Throwable throwable) {
            synchronized (AbstractActor.this) {
                if (completeSignalled) {
                    return;
                }
                if (throwable == null) {
                    throw new NullPointerException();
                }
                completeSignalled = true;
                completionException = throwable;
                unBlock();
            }
        }

        @Override
        public void onComplete() {
            synchronized (AbstractActor.this) {
                if (completeSignalled) {
                    return;
                }
                completeSignalled = true;
                unBlock();
            }
        }

        public synchronized T poll() {
            T res = item;
            item = null;
            if (!completeSignalled) {
                block();
            }
            return res;
        }

        public synchronized T remove() {
            T res = item;
            item = null;
            if (res == null) {
                if (completeSignalled) {
                    throw new NoSuchElementException(); // better should be CompletionException(
                } else {
                    throw new RuntimeException("Internal error");
                }
            }
            if (!completeSignalled) {
                block();
            }
            return res;
        }
    }

}
