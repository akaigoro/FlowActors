package org.df4j.reactiveactors;

import  java.util.concurrent.Flow.Publisher;
import  java.util.concurrent.Flow.Subscriber;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends AbstractActor implements Publisher<T>{
    protected ReactiveOutPort<T> outPort = new ReactiveOutPort<>();

    protected synchronized void complete() {
        super.complete();
        outPort.onComplete();
    }

    protected synchronized void completExceptionally(Throwable throwable) {
        super.completExceptionally(throwable);
        outPort.onError(throwable);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected abstract T whenNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void turn() throws Throwable {
        T res = whenNext();
        if (res == null) {
            complete();
        } else {
            outPort.onNext(res);
            restart();
        }
    }
}
