package org.df4j.reactiveactors;

import org.reactivestreams.Subscriber;

public abstract class AbstractSubscriber<T> extends org.df4j.plainactors.AbstractSubscriber<T> implements Subscriber<T> {

    protected void init() {
        inPort = new ReactiveInPort<>(this);
    }

    public ReactiveInPort<T> getInPort() {
        return (ReactiveInPort<T>) inPort;
    }

    @Override
    public void onNext(T item) {
        inPort.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        inPort.onError(throwable);
    }

    @Override
    public void onComplete() {
        inPort.onComplete();
    }
}
