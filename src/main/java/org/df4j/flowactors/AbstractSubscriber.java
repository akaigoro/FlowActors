package org.df4j.flowactors;

import java.util.concurrent.Flow;

public abstract class AbstractSubscriber<T> extends Actor implements Flow.Subscriber<T> {
    private InPort<T> inPort = new InPort<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
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
    protected void atComplete() {}
    protected void atError(Throwable throwable) {
        throwable.printStackTrace();
    }

    /** processes one data item
     */
    @Override
    protected void run() {
        try {
            if (!inPort.isCompleted()) {
                T item = inPort.remove();
                atNext(item);
                restart();
            } else {
                Throwable thr = inPort.getCompletionException();
                if (thr == null) {
                    atComplete();
                } else {
                    atError(thr);
                }
            }
        } catch (Throwable throwable) {
            inPort.onError(throwable);
        }
    }
}
