package org.df4j.reactiveactors;

import java.util.concurrent.Flow;

public abstract class AbstractSubscriber<T> extends org.df4j.plainactors.AbstractSubscriber<T> implements Flow.Subscriber<T> {

    protected void init() {
        inPort = new ReactiveInPort<>(this);
    }

    @Override
    public ReactiveInPort<T> getInPort() {
        return (ReactiveInPort<T>) super.getInPort();
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
