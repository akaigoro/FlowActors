package org.df4j.flowactors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends FlowActor implements Publisher<T>{
    protected ReactiveOutPort<T> outPort = new ReactiveOutPort<>();

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

    protected abstract T whenNext()  throws Throwable;

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
