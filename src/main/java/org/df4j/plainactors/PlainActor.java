package org.df4j.plainactors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;

public abstract class PlainActor {
    private Executor excecutor = ForkJoinPool.commonPool();
    private State state = State.CREATED;
    private int blocked = 0;
    protected Throwable globalCompletionException = null;

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

    protected synchronized void atComplete(Throwable cause) {
        state = State.COMPLETED;
        globalCompletionException = cause;
        notifyAll();
        if (cause == null) {
            whenComplete();
        } else {
            whenError(cause);
        }
    }

    protected void whenComplete() {}
    protected void whenError(Throwable throwable) {
        throwable.printStackTrace();
    }

    public synchronized void join(long timeout0) throws InterruptedException, TimeoutException {
        long timeout = timeout0;
        long targetTime = System.currentTimeMillis() + timeout;
        while (state!= State.COMPLETED && timeout > 0) {
            wait(timeout);
            timeout = targetTime - System.currentTimeMillis();
        }
        if (state!= State.COMPLETED) {
            throw new TimeoutException();
        }
    }


    public enum State {
        CREATED,
        RUNNING,
        COMPLETED
    }

    public class Port {
        boolean ready = false;

        public Port() {
            synchronized (PlainActor.this) {
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

    public class AsyncSemaPort extends Port {
        private long permissions = 0;

        public AsyncSemaPort(long permissions) {
            this.permissions = permissions;
        }

        public AsyncSemaPort() {
        }

        public synchronized void release(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            boolean doUnBlock = permissions == 0;
            permissions += n;
            if (permissions < 0) { // overflow
                permissions = Long.MAX_VALUE;
            }
            if (doUnBlock) {
                unBlock();
            }
        }

        public synchronized void aquire(long delta) {
            if (delta <= 0) {
                throw new IllegalArgumentException();
            }
            long newPermissionsValue = permissions-delta;
            if (newPermissionsValue < 0) {
                throw new IllegalArgumentException();
            }
            permissions = newPermissionsValue;
            if (permissions == 0) {
                block();
            }
        }

        public void aquire() {
            aquire(1);
        }
    }

    public class InPort<T> extends Port implements Subscriber<T> {
        protected Subscription subscription;
        private T item;
        protected boolean completeSignalled;
        protected Throwable completionException = null;

        public T current() {
            return item;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
        }

        public T poll() {
            synchronized (PlainActor.this) {
                T res = item;
                item = null;
                if (!completeSignalled) {
                    block();
                }
                return res;
            }
        }

        public T remove() {
            synchronized (PlainActor.this) {
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

        @Override
        public void onNext(T item) {
            if (item == null) {
                throw new NullPointerException();
            }
            synchronized (PlainActor.this) {
                if (completeSignalled) {
                    return;
                }
                this.item = item;
                unBlock();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            synchronized (PlainActor.this) {
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
            synchronized (PlainActor.this) {
                if (completeSignalled) {
                    return;
                }
                completeSignalled = true;
                unBlock();
            }
        }

        public boolean isCompleted() {
            return item==null && completeSignalled;
        }

        public Throwable getCompletionException() {
            return completionException;
        }
    }

    public class OutPort<T> implements Publisher<T>, Subscription {
        protected InPort<Subscriber<? super T>> subscriberPort = new InPort<>();

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            if (subscriber == null) {
                subscriber.onError(new NullPointerException());
                return;
            }
            this.subscriberPort.onNext(subscriber);
            subscriber.onSubscribe(this);
        }

        public void request(long n) {
            // do nothing
        }

        @Override
        public void cancel() {
            subscriberPort.poll();
        }

        public void onNext(T item) {
            subscriberPort.current().onNext(item);
        }

        public void onComplete() {
            Subscriber<? super T> current = subscriberPort.current();
            current.onComplete();
        }

        public void onError(Throwable throwable) {
            subscriberPort.current().onError(throwable);
        }
    }

}
