package org.df4j.reactiveactors;

import org.reactivestreams.Subscription;

import java.util.logging.Logger;

public class AbstractSubscriber<T> extends AbstractActor implements org.reactivestreams.Subscriber<T> {
    public ReactiveInPort<T> inPort = new ReactiveInPort<>();
    Logger logger = Logger.getLogger("consumer");

    @Override
    public void onSubscribe(Subscription subscription) {
        getInPort().onSubscribe(subscription);
    }

    public ReactiveInPort<T> getInPort() {
        return inPort;
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

    protected void whenNext(T item) throws Throwable {}

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
            whenNext(item);
        }
    }
}
