package org.df4j.reactiveactors;

import org.df4j.plainactors.AbstractTransformer;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends AbstractTransformer<T,R> implements Processor<T,R> {

    protected void init() {
        inPort = new ReactiveInPort<>(this);
        outPort = new ReactiveOutPort<>(this);
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
    public void subscribe(Subscriber<? super R> subscriber) {
        getOutPort().subscribe(subscriber);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
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

