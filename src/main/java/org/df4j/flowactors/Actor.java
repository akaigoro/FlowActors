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
            if (ready) {
                blocked++;
                ready = false;
            }
        }

        protected void unBlock() {
            if (ready) {
                return;
            }
            ready = true;
            blocked--;
            if (blocked>0) {
                return;
            }
            fire();
        }
    }

    public synchronized void join(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        join(unit.toMillis(timeout));
    }

    public synchronized void join(long timeout) throws InterruptedException, TimeoutException {
        long targetTime = System.currentTimeMillis() + timeout;
        while (state!=State.COMPLETED) {
            wait(timeout);
            timeout = targetTime - System.currentTimeMillis();
            if (timeout < 0) {
                throw new TimeoutException();
            }
        }
    }


    class OutPort<T> extends Port implements Flow.Publisher<T>, Flow.Subscription {
        Flow.Subscriber<? super T> subscriber;
        int requested=0;

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
        public synchronized void request(long n) {
            if (subscriber == null) {
                return;
            }
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            boolean doUnBlock = requested==0;
            requested+=n;
            if (doUnBlock) {
                unBlock();
            }
        }

        @Override
        public synchronized void cancel() {
            if (subscriber == null) {
                return;
            }
            subscriber = null;
        }

        public void onNext(T item) {
            synchronized (Actor.this) {
                if (subscriber == null) {
                    return;
                }
                if (requested == 0) {
                    subscriber.onError(new IllegalStateException());
                }
                requested--;
                if (requested == 0) {
                    block();
                }
            }
            subscriber.onNext(item);
        }

        public void onComplete() {
            synchronized (Actor.this) {
                if (subscriber == null) {
                    throw new IllegalStateException();
                }
            }
            subscriber.onComplete();
        }

        public void onError(Throwable throwable) {
            synchronized (Actor.this) {
                if (subscriber == null) {
                    throw new IllegalStateException();
                }
            }
            subscriber.onError(throwable);
        }
    }

    class InPort<T> extends Port implements Flow.Subscriber<T>  {
        private Flow.Subscription subscription;
        private T item;
        private boolean completed;
        private Throwable completionException = null;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {

            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(T item) {
            synchronized (Actor.this) {
                if (completed) {
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
                if (completed) {
                    return;
                }
                if (throwable == null) {
                    throw new NullPointerException();
                }
                completed = true;
                completionException = throwable;
                unBlock();
            }
        }

        @Override
        public void onComplete() {
            synchronized (Actor.this) {
                if (completed) {
                    return;
                }
                completed = true;
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

        public T remove() {
            T res;
            synchronized (Actor.this) {
                if (item == null || subscription == null) {
                    throw new IllegalStateException();
                }
                res = item;
                item = null;
                subscription.request(1);
                block();
                if (completed) {
                    state =  State.COMPLETED;
                    Actor.this.notifyAll();
                }
            }
            return res;
        }
    }
}
