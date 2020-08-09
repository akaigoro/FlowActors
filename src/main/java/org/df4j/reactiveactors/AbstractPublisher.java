package org.df4j.reactiveactors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends AbstractActor implements Publisher<T> {

    public ReactiveOutPort<T> outPort = new ReactiveOutPort<>();

    public ReactiveOutPort<T> getOutPort() {
        return outPort;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        getOutPort().subscribe(subscriber);
    }

    protected synchronized void whenComplete() {
        super.whenComplete();
        outPort.onComplete();
    }

    protected synchronized void whenError(Throwable throwable) {
        super.whenError(throwable);
        outPort.onError(throwable);
    }

    protected abstract T whenNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void turn() throws Throwable {
        T res = whenNext();
        if (res == null) {
            whenComplete();
        } else {
            outPort.onNext(res);
        }
    }
}
