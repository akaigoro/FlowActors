package org.df4j.flowactors;

import java.util.concurrent.Flow;
import  java.util.concurrent.Flow.Processor;
import  java.util.concurrent.Flow.Subscription;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends AbstractActor implements Processor<T, R>, Flow.Publisher<R> {
    protected ReactiveInPort<T> inPort = new ReactiveInPort<>();
    protected ReactiveOutPort<R> outPort = new ReactiveOutPort<>();

    @Override
    public void onSubscribe(Subscription subscription) {
        inPort.onSubscribe(subscription);
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

    protected synchronized void complete() {
        super.complete();
        outPort.onComplete();
    }

    protected synchronized void completExceptionally(Throwable throwable) {
        super.completExceptionally(throwable);
        outPort.onError(throwable);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        outPort.subscribe(subscriber);
    }

    /**
     *
     * @param item input data
     * @return processed data
     * @throws Throwable if something went wrong
     */
    protected abstract R whenNext(T item)  throws Throwable;

    protected void whenComplete() {}

    protected void whenError(Throwable throwable) {}

    /** processes one data item
     */
    @Override
    protected void turn() throws Throwable {
        if (inPort.isCompletedExceptionally()) {
            whenError(inPort.getCompletionException());
        } else  if (inPort.isCompleted()) {
            whenComplete();
        } else {
            T item = inPort.poll();
            R res = whenNext(item);
            if (res == null) {
                complete();
            } else {
                outPort.onNext(res);
                restart();
            }
        }
    }
}

