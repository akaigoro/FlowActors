package org.df4j.core.port;

import org.df4j.core.dataflow.Actor;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlow<T> extends CompletablePort implements Flow.Subscriber<T> {
    private int bufferCapacity;
    private final ArrayDeque<T> tokens;
    protected Flow.Subscription subscription;
    private long requestedCount;

    /**
     * creates a port which is subscribed to the {@code #Flow.Publisher}
     * @param parent {@link AsyncProc} to wich this port belongs
     * @param capacity required capacity
     */
    public InpFlow(Actor parent, int capacity) {
        super(parent);
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        bufferCapacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    private boolean buffIsFull() {
        return tokens.size() == bufferCapacity;
    }

    private long remainingCapacity() {
        if (requestedCount < 0) {
            throw new IllegalStateException();
        }
        long res = bufferCapacity- tokens.size() - requestedCount;
        if (res < 0) {
            throw new IllegalStateException();
        }
        return res;
    }

    public boolean isCompleted() {
        synchronized(parent) {
            return completed && tokens.isEmpty();
        }
    }

    public synchronized T current() {
        return tokens.peek();
    }

    @Override
    public synchronized void onSubscribe(Flow.Subscription subscription) {
        synchronized (this) {
            if (this.subscription != null) {
                subscription.cancel(); // this is dictated by the spec.
                return;
            }
            this.subscription = subscription;
            if (tokens.isEmpty()) {
                block();
            }
            requestedCount = remainingCapacity();
            if (requestedCount == 0) {
                return;
            }
        }
        subscription.request(requestedCount);
    }

    /**
     * normally this method is called by Flow.Publisher.
     * But before the port is subscribed, this method can be called directly.
     * @throws IllegalArgumentException when argument is null
     * @throws IllegalStateException if no room left to store argument
     * @param message token to store
     */
    @Override
    public void onNext(T message) {
        synchronized(this) {
            if (message == null) {
                throw new NullPointerException();
            }
            if (isCompleted()) {
                return;
            }
            if (subscription != null) {
                requestedCount--;
            }
            tokens.add(message);
            unblock();
        }
    }

    public T poll() {
        long n;
        T res;
        synchronized(parent) {
            if (!ready) {
                throw new IllegalStateException();
            }
            res = tokens.poll();
            if (tokens.isEmpty() && !completed) {
                block();
            }
            if (subscription == null) {
                return res;
            }
            n = remainingCapacity();
            requestedCount += n;
        }
        subscription.request(n);
        return res;
    }

    public T remove() throws CompletionException {
        if (isCompleted()) {
            throw new java.util.concurrent.CompletionException(completionException);
        }
        T res = poll();
        return res;
    }
}
