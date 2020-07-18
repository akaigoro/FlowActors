package org.df4j.flowactors;

import java.util.concurrent.Flow;

/**
 * minimalistic {@link Flow.Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends Actor implements Flow.Publisher<T>{
    private OutPort<T> outPort = new OutPort<>();

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected abstract T atNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void run() {
        try {
            T res = atNext();
            if (res!=null) {
                outPort.onNext(res);
                restart();
            } else {
                outPort.onComplete();
            }
        } catch (Throwable throwable) {
            outPort.onError(throwable);
        }
    }
}
