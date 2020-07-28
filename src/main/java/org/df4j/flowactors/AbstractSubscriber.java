package org.df4j.flowactors;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.NoSuchElementException;

public abstract class AbstractSubscriber<T> extends FlowActor implements Subscriber<T> {
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
    protected void whenError(Throwable throwable) {
        throwable.printStackTrace();
    }

    /** processes one data item
     */
    @Override
    protected void run() {
        T item;
        try {
            item = inPort.remove();
        } catch (NoSuchElementException throwable) {
            if (!inPort.isCompleted()) {
                throw new RuntimeException("Internal error");
            }
            atComplete();
            Throwable thr = inPort.getCompletionException();
            if (thr == null) {
                whenComplete();
            } else {
                whenError(thr);
            }
            return;
        }
        try {
            whenNext(item);
        } catch (Throwable throwable) {
            whenError(throwable);
            return;
        }
        restart();
    }
}
