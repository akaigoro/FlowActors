package org.df4j.reactiveactors;

import org.reactivestreams.Subscriber;

public abstract class AbstractSubscriber<T> extends AbstractActor implements Subscriber<T> {

    public InPort<T> inPort;

    protected void init() {
        inPort = new ReactiveInPort<>();
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

    protected abstract void whenNext(T item) throws Throwable;

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
