package org.df4j.flowactors;

import java.util.concurrent.*;

public abstract class Actor {
    private Executor excecutor = ForkJoinPool.commonPool();
    private State state = State.CREATED;
    private int blocked = 0;
    /**
     * blocked initially and when running.
     */
    private Port controlPort = new Port();

    private void fire() {
        controlPort.block();
        excecutor.execute(this::run);
    }

    public synchronized void start() {
        if (state != State.CREATED) {
            throw new IllegalStateException();
        }
        state = State.RUNNING;
        controlPort.unBlock();
    }

    protected synchronized void restart() {
        controlPort.unBlock();
    }

    protected abstract void run();

    public boolean isCompleted() {
        return state == State.COMPLETED;
    }

    public enum State {
        CREATED,
        RUNNING,
        COMPLETED
    }

    class Port {
        boolean ready = false;

        public Port() {
            synchronized (Actor.this) {
                blocked++;
            }
        }

        /**
         * under synchronized (Actor.this)
         */
        protected void block() {
            if (!ready) {
                return;
            }
            ready = false;
            blocked++;
        }

        protected void unBlock() {
            if (ready) {
                return;
            }
            ready = true;
            blocked--;
            if (blocked == 0) {
                fire();
            }
        }
    }

    public synchronized void join(long timeout0) throws InterruptedException, TimeoutException {
        long timeout = timeout0;
        long targetTime = System.currentTimeMillis() + timeout;
        while (state!=State.COMPLETED && timeout > 0) {
            wait(timeout);
            timeout = targetTime - System.currentTimeMillis();
        }
        if (state!=State.COMPLETED) {
            throw new TimeoutException();
        }
    }

    public class AsyncSemaPort extends Port implements AsyncSema {
        private int permissions;

        public AsyncSemaPort(int permissions) {
            this.permissions = permissions;
        }

        public AsyncSemaPort() {
            this(0);
        }

        @Override
        public synchronized void release(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            boolean doUnBlock = permissions == 0;
            permissions += n;
            if (doUnBlock) {
                unBlock();
            }
        }

        protected synchronized void aquire() {
            if (permissions == 0) {
                throw new IllegalStateException();
            }
            permissions--;
            if (permissions == 0) {
                block();
            }
        }
    }

    public class OutPort<T> implements Flow.Publisher<T>, Flow.Subscription {
        Flow.Subscriber<? super T> subscriber;

        @Override
        public void subscribe(Flow.Subscriber<? super T> subscriber) {
            if (subscriber == null) {
                throw new NullPointerException();
            }
            if (this.subscriber != null) {
                subscriber.onError(new IllegalStateException());
            }
            this.subscriber = subscriber;
            subscriber.onSubscribe(this);
        }

        @Override
        public void request(long n) {
        }

        @Override
        public synchronized void cancel() {
            if (subscriber == null) {
                return;
            }
            subscriber = null;
        }

        public boolean onNext(T item) {
            synchronized (Actor.this) {
                if (subscriber == null) {
                    return false;
                }
            }
            subscriber.onNext(item);
            return true;
        }

        public void onComplete() {
            synchronized (Actor.this) {
                if (subscriber == null) {
                    return;
                }
            }
            subscriber.onComplete();
        }

        public void onError(Throwable throwable) {
            synchronized (Actor.this) {
                if (subscriber == null) {
                    return;
                }
            }
            subscriber.onError(throwable);
        }
    }

    public class ReactiveOutPort<T> extends OutPort<T> {
        AsyncSemaPort sema = new AsyncSemaPort();

        @Override
        public synchronized void request(long n) {
            if (subscriber == null) {
                return;
            }
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            sema.release(n);
        }

        public boolean onNext(T item) {
            synchronized (Actor.this) {
                if (subscriber == null) {
                    return false;
                }
                sema.aquire();
            }
            subscriber.onNext(item);
            return true;
        }
    }

    public class ReactiveInPort<T> extends Port implements Flow.Subscriber<T> {
        protected Flow.Subscription subscription;
        protected T item;
        protected boolean completeSignalled;
        protected Throwable completionException = null;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(1);
        }

        public T poll() {
            synchronized (Actor.this) {
                if (subscription == null) {
                    throw new IllegalStateException();
                }
                T res = item;
                item = null;
                if (completeSignalled) {
                    state =  State.COMPLETED;
                    Actor.this.notifyAll();
                } else {
                    subscription.request(1);
                    block();
                }
                return res;
            }
        }

        @Override
        public void onNext(T item) {
            synchronized (Actor.this) {
                if (completeSignalled) {
                    return;
                }
                if (item == null) {
                    throw new NullPointerException();
                }
                this.item = item;
                unBlock();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            synchronized (Actor.this) {
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
            synchronized (Actor.this) {
                if (completeSignalled) {
                    return;
                }
                completeSignalled = true;
                if (item == null) {
                    state = State.COMPLETED;
                    Actor.this.notifyAll();
                }
                unBlock();
            }
        }

        public boolean isCompleted() {
            return state == State.COMPLETED;
        }

        public Throwable getCompletionException() {
            return completionException;
        }
    }
}
