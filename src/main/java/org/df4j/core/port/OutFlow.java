package org.df4j.core.port;

import org.df4j.core.dataflow.Actor;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 * Is ready when has room to store at least one toke
 * @param <T> type of emitted tokens
 */
public class OutFlow<T> extends CompletablePort implements Flow.Publisher<T> {
    public static final int DEFAULT_CAPACITY = 16;
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    private SubscriptionQueue activeSubscribtions = new SubscriptionQueue();
    private SubscriptionQueue passiveSubscribtions = new SubscriptionQueue();

    public OutFlow(Actor parent, int capacity) {
        super(parent, capacity>0);
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public OutFlow(Actor parent) {
        this(parent, DEFAULT_CAPACITY);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(subscriber);
        synchronized(parent) {
            if (passiveSubscribtions != null) {
                passiveSubscribtions.add(subscription);
            }
        }
        subscriber.onSubscribe(subscription);
        synchronized(parent) {
            if (isCompleted()) {
                subscription.onComplete();
            }
        }
    }

    /**
     * how many tokens can be stored in the buffer
     * @return 0 if buffer is full
     */
    private int _remainingCapacity() {
        return capacity - tokens.size();
    }

    /**
     *
     * @param token token to insert
     */
    public void onNext(T token) {
        boolean result = false;
        boolean finished = false;
        if (token == null) {
            throw new NullPointerException();
        }
        FlowSubscriptionImpl sub;
        synchronized(parent) {
            if (!completed) {
                sub = activeSubscribtions.poll();
                if (sub == null) {
                    if (_remainingCapacity() == 0) {
                        finished = true;
                    } else {
                        tokens.add(token);
                        hasItemsEvent();
                        if (_remainingCapacity() == 0) {
                            block();
                        }
                    }
                } else {
                    boolean subIsActive = sub.onNext(token);
                    if (!sub.isCancelled()) {
                        if (subIsActive) {
                            activeSubscribtions.add(sub);
                        } else {
                            passiveSubscribtions.add(sub);
                        }
                    }
                    if (_remainingCapacity() == 0 && activeSubscribtions.isEmpty()) {
                        block();
                    }
                }
                if (!finished) {
                    result = true;
                }
            }
        }
        if (!result) {
            throw new IllegalStateException("buffer overflow");
        }
    }

    public boolean isCompleted() {
        synchronized(parent) {
            return completed && tokens.size() == 0;
        }
    }

    public void hasItemsEvent() {
        parent.notifyAll();
    }

    private void completAllSubscriptions() {
        for (;;) {
            FlowSubscriptionImpl sub = activeSubscribtions.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
        for (;;) {
            FlowSubscriptionImpl sub = passiveSubscribtions.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
    }

    public void _onComplete(Throwable cause) {
        synchronized(parent) {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            hasItemsEvent();
            if (tokens.size() > 0) {
                return;
            }
            completAllSubscriptions();
        }
    }

    public T poll() {
        synchronized(parent) {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                return null;
            }
        }
    }

    protected class FlowSubscriptionImpl implements Flow.Subscription {
        protected Flow.Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;
        private FlowSubscriptionImpl prev = this;
        private FlowSubscriptionImpl next = this;

        FlowSubscriptionImpl() { }

        FlowSubscriptionImpl(Flow.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        public FlowSubscriptionImpl getPrev() {
            return prev;
        }

        public void setPrev(FlowSubscriptionImpl prev) {
            this.prev = prev;
        }

        public boolean isLinked() {
            return getNext() != this;
        }

        public void unlink() {
            getPrev().setNext(this.getNext());
            getNext().setPrev(this.getPrev());
            this.setPrev(this);
            this.setNext(this);
        }

        public boolean isCancelled() {
            synchronized(parent) {
                return cancelled;
            }
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            T token;
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                remainedRequests += n;
                if (remainedRequests > n) {
                    return;
                }
                if (isCompleted()) {
                    this.onComplete();
                    return;
                }
                // remainedRequests was 0, so this subscription was passive
                passiveSubscribtions.remove(this);
                for (;;) {
                    token = OutFlow.this.poll();
                    if (token == null) {
                        activeSubscribtions.add(this);
                        unblock();
                        break;
                    }
                    boolean subIsActive = this.onNext(token);
                    if (!subIsActive) {
                        passiveSubscribtions.add(this);
                        break;
                    }
                    if (isCompleted()) {
                        this.onComplete();
                        break;
                    }
                }
                if (isCompleted()) {
                    completAllSubscriptions();
                }
            }
        }

        @Override
        public synchronized void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            synchronized(parent) {
                if (remainedRequests > 0) {
                    activeSubscribtions.remove(this);
                } else {
                    passiveSubscribtions.remove(this);
                }
            }
        }

        /**
         * must be unlinked
         * @param token token to pass
         * @return
         */
        private boolean onNext(T token) {
            subscriber.onNext(token);
            synchronized(this) {
                remainedRequests--;
                return remainedRequests > 0;
            }
        }

        /**
         * must be unlinked
         * @param cause error
         */
        private void onComplete() {
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
            }
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }

        public FlowSubscriptionImpl getNext() {
            return next;
        }

        public void setNext(FlowSubscriptionImpl next) {
            this.next = next;
        }
    }

    public class SubscriptionQueue {
        private OutFlow.FlowSubscriptionImpl header = new OutFlow.FlowSubscriptionImpl();
        private int size = 0;

        public synchronized boolean isEmpty() {
            return size == 0;
        }

        public synchronized boolean add(FlowSubscriptionImpl flowSubscription) {
            if (flowSubscription == header) {
                throw new IllegalArgumentException();
            }
            flowSubscription.setNext(header);
            FlowSubscriptionImpl prev = header.getPrev();
            flowSubscription.setPrev(prev);
            prev.setNext(flowSubscription);
            header.setPrev(flowSubscription);
            size++;
            return true;
        }

        public synchronized FlowSubscriptionImpl poll() {
            if (size == 0) {
                return null;
            }
            size--;
            OutFlow.FlowSubscriptionImpl first = null;
            OutFlow.FlowSubscriptionImpl res = header.getNext();
            if (res != header) {
                res.unlink();
                first = res;
            }
            if (first == null) {
                return  null;
            } else {
                return (FlowSubscriptionImpl)first;
            }
        }

        public synchronized boolean remove(OutFlow.FlowSubscriptionImpl subscription) {
            if (subscription.isLinked()) {
                subscription.unlink();
                size--;
                return true;
            } else {
                return false;
            }
        }
    }
}
