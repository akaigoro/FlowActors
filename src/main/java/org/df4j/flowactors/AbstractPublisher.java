package org.df4j.flowactors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends Actor implements Publisher<T>{
    protected ReactiveOutPort<T> outPort = new ReactiveOutPort<>();

    protected void atComplete() {
        super.atComplete();
        outPort.onComplete();
    }
    protected void atError(Throwable throwable) {
        outPort.onError(throwable);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected abstract T atNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void run() {
        try {
            T res = atNext();
            if (res==null) {
                atComplete();
            } else {
                outPort.onNext(res);
                restart();
            }
        } catch (Throwable throwable) {
            atError(throwable);
        }
    }
}
