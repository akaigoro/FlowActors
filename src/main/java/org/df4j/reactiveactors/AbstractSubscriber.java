package org.df4j.reactiveactors;

import  java.util.concurrent.Flow.Subscriber;
import  java.util.concurrent.Flow.Subscription;

public abstract class AbstractSubscriber<T> extends AbstractActor implements Subscriber<T> {
    protected ReactiveInPort<T> inPort = new ReactiveInPort<>();

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

    protected abstract void whenNext(T item) throws Throwable;

    protected void whenComplete() {}

    protected void whenError(Throwable throwable) {}

    /** processes one data item
     */
    @Override
    protected void turn() throws Throwable {
        if (inPort.isCompletedExceptionally()) {
            Throwable completionException = inPort.getCompletionException();
            completExceptionally(completionException);
            whenError(completionException);
        } else  if (inPort.isCompleted()) {
            complete();
            whenComplete();
        } else {
            T item = inPort.poll();
            if (item==null) {
                throw new RuntimeException("internal error");
            }
            whenNext(item);
            restart();
        }
    }
}
