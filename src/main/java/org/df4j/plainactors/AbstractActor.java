package org.df4j.plainactors;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
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
        if (!isCompleted()) {
            controlPort.unBlock();
        }
    }

    protected abstract void turn() throws Throwable;


    public synchronized boolean isCompleted() {
        return state == State.COMPLETED;
    }

    protected synchronized void complete() {
        state = State.COMPLETED;
        notifyAll();
    }

    protected synchronized void completExceptionally(Throwable throwable) {
        completionException = throwable;
        state = State.COMPLETED;
        notifyAll();
    }

    protected void run() {
        try {
            turn();
        } catch (Throwable throwable) {
            completExceptionally(throwable);
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
        boolean ready = false;

        public Port() {
            synchronized (AbstractActor.this) {
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
            release(permissions);
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
            synchronized (AbstractActor.this) {
                T res = item;
                item = null;
                if (!completeSignalled) {
                    block();
                }
                return res;
            }
        }

        public T remove() {
            synchronized (AbstractActor.this) {
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
            synchronized (AbstractActor.this) {
                if (completeSignalled) {
                    return;
                }
                if (this.item != null) {
                    throw new IllegalStateException();
                }
                this.item = item;
                unBlock();
            }
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

        public boolean isCompleted() {
            return item==null && completeSignalled;
        }

        public boolean isCompletedExceptionally() {
            return item==null && completeSignalled && completionException != null;
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
            subscriberPort.current().onComplete();
        }

        public void onError(Throwable throwable) {
            subscriberPort.current().onError(throwable);
        }
    }


    public class ReactiveInPort<T> extends InPort<T> {

        @Override
        public void onSubscribe(Subscription subscription) {
            super.onSubscribe(subscription);
            request(1);
        }

        public void request(long n) {
            subscription.request(n);
        }

        @Override
        public T poll() {
            T res = super.poll();
            request(1);
            return res;
        }

        @Override
        public T remove() {
            T res = super.remove();
            request(1);
            return res;
        }
    }

    public class ReactiveOutPort<T> extends OutPort<T> {
        AsyncSemaPort sema = new AsyncSemaPort();

        @Override
        public void request(long n) {
            Subscriber<? super T> subscriber;
            synchronized (AbstractActor.this) {
                subscriber = subscriberPort.current();
                if (subscriber == null) {
                    return;
                }
                try {
                    sema.release(n);
                } catch (IllegalArgumentException e) {
                    subscriber.onError(e);
                }
            }
        }

        public void onNext(T item) {
            Subscriber<? super T> subscriber;
            synchronized (AbstractActor.this) {
                subscriber = subscriberPort.current();
                if (subscriber == null) {
                    return;
                }
                try {
                    sema.aquire();
                } catch (IllegalArgumentException e) {
                    subscriber.onError(e);
                    return;
                }
            }
            subscriber.onNext(item);
        }
    }

}
