package org.df4j.flowactors;

import java.util.concurrent.Flow;

public abstract class ProcessorActor<T, R> extends Actor implements Flow.Processor<T, R> {
    protected InPort<T> inPort = new InPort<>();
    protected OutPort<R> outPort = new OutPort<>();

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

    protected void atComplete() {
        outPort.onComplete();
    }
    protected void atError(Throwable throwable) {
        outPort.onError(throwable);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected abstract R atNext(T item)  throws Throwable;

    @Override
    protected void run() {
        try {
            if (!inPort.isCompleted()) {
                T item = inPort.remove();
                R res= atNext(item);
                if (res!=null) {
                    outPort.onNext(res);
                    restart();
                } else {
                    outPort.onComplete();
                }
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
