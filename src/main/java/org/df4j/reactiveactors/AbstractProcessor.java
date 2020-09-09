package org.df4j.reactiveactors;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends AbstractActor implements Processor<T,R> {

    public ReactiveInPort<T> inPort = new ReactiveInPort<>();
    public ReactiveOutPort<R> outPort = new ReactiveOutPort<>();

    public ReactiveInPort<T> getInPort() {
        return inPort;
    }

    public ReactiveOutPort<R> getOutPort() {
        return outPort;
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

    protected synchronized void whenComplete() {
        super.whenComplete();
        outPort.onComplete();
    }

    protected synchronized void whenError(Throwable throwable) {
        super.whenError(throwable);
        outPort.onError(throwable);
    }

    /**
     *
     * @param item input data
     * @return processed data
     * @throws Throwable if something went wrong
     */
    protected abstract R whenNext(T item)  throws Throwable;

    /** processes one data item
     */
    @Override
    protected void turn() throws Throwable {
        if (inPort.isCompletedExceptionally()) {
            Throwable completionException = inPort.getCompletionException();
            whenError(completionException);
        } else  if (inPort.isCompleted()) {
            whenComplete();
        } else {
            T item = inPort.poll();
            if (item==null) {
                throw new RuntimeException();
            }
            R res = whenNext(item);
            if (res == null) {
                whenComplete();
            } else {
                outPort.onNext(res);
            }
        }
    }
}

