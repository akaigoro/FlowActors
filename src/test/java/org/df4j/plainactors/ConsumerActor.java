package org.df4j.plainactors;

import org.df4j.flowactors.FlowActor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.NoSuchElementException;

public abstract class ConsumerActor<T> extends PlainActor implements Subscriber<T> {
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

    protected abstract void atNext(T item) throws Throwable;
    protected void atError(Throwable throwable) {
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
            Throwable thr = inPort.getCompletionException();
            if (thr == null) {
                atComplete();
            } else {
                atError(thr);
            }
            return;
        }
        try {
            atNext(item);
        } catch (Throwable throwable) {
            atError(throwable);
            return;
        }
        restart();
    }
}