package org.df4j.reactiveactors;

import org.df4j.plainactors.OutMessagePort;

import java.util.concurrent.Flow;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends org.df4j.plainactors.AbstractProcessor<T,R> implements Flow.Processor<T,R> {

    protected void init() {
        inPort = new ReactiveInPort<>(this);
        outPort = new ReactiveOutPort<R>(this);
    }

    @Override
    public ReactiveInPort<T> getInPort() {
        return (ReactiveInPort<T>) super.getInPort();
    }

    @Override
    public ReactiveOutPort<R> getOutPort() {
        return (ReactiveOutPort<R>) super.getOutPort();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        getOutPort().subscribe(subscriber);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        getInPort().onSubscribe(subscription);
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

