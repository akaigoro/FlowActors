package org.df4j.flowactors;

import java.util.concurrent.Flow;

public abstract class PublisherActor<T> extends Actor implements Flow.Publisher<T>{
    protected OutPort<T> outPort = new OutPort<>();

    @Override
    protected void run() {
        try {
            T res = atNext();
            if (res!=null) {
                outPort.onNext(res);
            } else {
                outPort.onComplete();
            }
        } catch (Throwable throwable) {
            outPort.onError(throwable);
        }
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected abstract T atNext()  throws Throwable;
}
