package org.df4j.plainactors;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public abstract class AbstractSubscriber<T> extends AbstractActor implements Subscriber<T> {
    protected InPort<T> inPort = new InPort<>();

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
            whenError(inPort.getCompletionException());
        } else  if (inPort.isCompleted()) {
            whenComplete();
        } else {
            T item = inPort.poll();
            if (item==null) {
                throw new RuntimeException();
            }
            whenNext(item);
            restart();
        }
    }
}
