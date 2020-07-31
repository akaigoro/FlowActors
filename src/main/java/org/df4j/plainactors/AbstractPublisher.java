package org.df4j.plainactors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.logging.Logger;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 */
public abstract class AbstractPublisher<T> extends PlainActor implements Publisher<T>{
    Logger logger = Logger.getLogger("producer");
    protected OutPort<T> outPort = new OutPort<>();

    protected void atComplete(Throwable cause) {
        super.atComplete(cause);
        if (cause == null) {
            outPort.onComplete();
        } else {
            outPort.onError(cause);
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        outPort.subscribe(subscriber);
    }

    long cnt = 0;
    protected abstract T whenNext();

    /** generates one data item
     */
    @Override
    protected void run() {
        try {
            T res = whenNext();
            if (res==null) {
                atComplete(null);
            } else {
                outPort.onNext(res);
                restart();
            }
        } catch (Throwable throwable) {
            atComplete(throwable);
        }
    }
}
