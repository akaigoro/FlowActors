package org.df4j.concurrent.flow;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
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
            return !isBlocked;
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

    public class InPort<T> extends Port {
        private final int capacity;
        private final ArrayDeque<T> items;
        protected boolean completeSignalled;
        protected Throwable completionException = null;

        public InPort(int capacity) {
            this.capacity = capacity;
            items = new ArrayDeque<>(capacity);
        }

        public InPort() {
            this(8);
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }

        @Override
        public boolean isFull() {
            return items.size() == capacity;
        }

        public T current() {
            return items.peek();
        }

        public boolean isCompleted() {
            return isEmpty() && completeSignalled;
        }

        public boolean isCompletedExceptionally() {
            return isEmpty() && completeSignalled && completionException != null;
        }

        public Throwable getCompletionException() {
            return completionException;
        }

        public void onNext(T item) {
            if (item == null) {
                throw new NullPointerException();
            }
            if (completeSignalled) {
                return;
            }
            if (isFull()) {
                throw new IllegalStateException();
            }
            this.items.add(item);
            unBlock();
        }

        public void onError(Throwable throwable) {
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

        public synchronized void onComplete() {
            if (completeSignalled) {
                return;
            }
            completeSignalled = true;
            unBlock();
        }

        public synchronized T poll() {
            T res = items.poll();
            if (isEmpty() && !completeSignalled) {
                block();
            }
            return res;
        }

        public synchronized T remove() {
            T res = poll();
            if (res != null) {
                return res;
            } else  if (completeSignalled) {
                throw new NoSuchElementException(); // better should be CompletionException(
            } else {
                throw new RuntimeException("Internal error");
            }
        }
    }

    public class ReactiveInPort<T> extends Port implements Subscriber<T> {
        private final int capacity;
        private final ArrayDeque<T> items;
        protected Subscription subscription;
        protected boolean completeSignalled;
        protected Throwable completionException = null;

        public ReactiveInPort(int capacity) {
            this.capacity = capacity;
            items = new ArrayDeque<>(capacity);
        }

        public ReactiveInPort() {
            this(8);
        }

        @Override
        public synchronized void onSubscribe(Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            int freeSpace = capacity - items.size();
            if (freeSpace > 0) {
                subscription.request(freeSpace);
            }
        }

        public T poll() {
            T result;
            boolean extracted;
            synchronized (this) {
                int size0 = items.size();
                result = items.poll();
                extracted = size0 > items.size();
                if (isEmpty() && !completeSignalled) {
                    block();
                }
            }
            if (extracted) {
                subscription.request(1);
            }
            return result;
        }

        public synchronized T remove() {
            T result = poll();
            if (result != null) {
                return result;
            } else  if (completeSignalled) {
                throw new NoSuchElementException(); // better should be CompletionException(
            } else {
                throw new RuntimeException("Internal error");
            }
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }

        @Override
        public boolean isFull() {
            return items.size() == capacity;
        }

        public T current() {
            return items.poll();
        }

        public boolean isCompleted() {
            return isEmpty() && completeSignalled;
        }

        public boolean isCompletedExceptionally() {
            return isEmpty() && completeSignalled && completionException != null;
        }

        public Throwable getCompletionException() {
            return completionException;
        }

        public void onNext(T item) {
            if (item == null) {
                throw new NullPointerException();
            }
            if (completeSignalled) {
                return;
            }
            if (isFull()) {
                throw new IllegalStateException();
            }
            this.items.add(item);
            unBlock();
        }

        public void onError(Throwable throwable) {
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

        public synchronized void onComplete() {
            if (completeSignalled) {
                return;
            }
            completeSignalled = true;
            unBlock();
        }
    }

    public class ReactiveOutPort<T> implements Publisher<T>, Subscription {
        protected InPort<Subscriber<? super T>> subscriber = new InPort<>();
        protected AsyncSemaPort sema = new AsyncSemaPort();

        @Override
        public synchronized void subscribe(Subscriber<? super T> subscriber) {
            if (subscriber == null) {
                throw new NullPointerException();
            }
            if (this.subscriber.isFull()) {
                subscriber.onError(new IllegalStateException());
                return;
            }
            this.subscriber.onNext(subscriber);
            subscriber.onSubscribe(this);
        }

        @Override
        public synchronized void request(long n) {
            if (subscriber.isEmpty()) {
                return; // this spec requirement
            }
            if (n <= 0) {
                subscriber.current().onError(new IllegalArgumentException());
                return;
            }
            sema.release(n);
        }

        public synchronized void onNext(T item) {
            Subscriber<? super T> subscriber = this.subscriber.current();
            if (subscriber == null) {
                throw  new IllegalStateException();
            }
            sema.aquire();
            subscriber.onNext(item);
        }

        @Override
        public synchronized void cancel() {
            Subscriber<? super T> subscriber = this.subscriber.current();
            if (subscriber == null) {
                return;
            }
            this.subscriber.remove();
            this.sema.setPermissions(0);
        }

        public synchronized void onComplete() {
            Subscriber<? super T> subscriber = this.subscriber.poll();
            if (subscriber == null) {
                return;
            }
            subscriber.onComplete();
        }

        public synchronized void onError(Throwable throwable) {
            Subscriber<? super T> subscriber = this.subscriber.poll();
            if (subscriber == null) {
                return;
            }
            subscriber.onError(throwable);
        }
    }
}
