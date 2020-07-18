package org.df4j.flowactors;

import java.util.concurrent.Flow;

public abstract class PublisherActor<T> extends Actor implements Flow.Publisher<T>{
    protected OutPort<T> outPort = new OutPort<>();

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected abstract T atNext()  throws Throwable;

    @Override
    protected void run() {
        try {
            T res = atNext();
            if (res!=null) {
                outPort.onNext(res);
                restart();
            } else {
                outPort.onComplete();
            }
        } catch (Throwable throwable) {
            outPort.onError(throwable);
        }
    }
}
