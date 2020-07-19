package org.df4j.flowactors;

import java.util.concurrent.Flow;

/**
 * minimalistic {@link Flow.Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends Actor implements Flow.Publisher<T>, Flow.Subscription {
    private Port outPort = new Port();

    Flow.Subscriber<? super T> subscriber;
    int requested=0;

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }
        synchronized (this) {
            if (this.subscriber != null) {
                subscriber.onError(new IllegalStateException());
            }
            this.subscriber = subscriber;
        }
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
            outPort.unBlock();
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
        synchronized (this) {
            if (subscriber == null) {
                return;
            }
            if (requested == 0) {
                subscriber.onError(new IllegalStateException());
            }
            requested--;
            if (requested == 0) {
                outPort.block();
            }
        }
        subscriber.onNext(item);
    }

    public void onComplete() {
        Flow.Subscriber<? super T> sub;
        synchronized (this) {
            if (subscriber == null) {
                throw new IllegalStateException();
            }
            sub = subscriber;
        }
        sub.onComplete();
    }

    public void onError(Throwable throwable) {
        Flow.Subscriber<? super T> sub;
        synchronized (this) {
            if (subscriber == null) {
                throw new IllegalStateException();
            }
            sub = subscriber;
        }
        sub.onError(throwable);
    }

    protected abstract T atNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void run() {
        try {
            T res = atNext();
            if (res!=null) {
                onNext(res);
                restart();
            } else {
                onComplete();
            }
        } catch (Throwable throwable) {
            onError(throwable);
        }
    }
}
