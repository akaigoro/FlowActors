package org.df4j.flowactors;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.NoSuchElementException;

/**
 * To make concrete processor, the method {@link AbstractProcessor##whenNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends FlowActor implements Processor<T, R>, org.reactivestreams.Publisher<R> {
    protected ReactiveInPort<T> inPort = new ReactiveInPort<>();
    protected ReactiveOutPort<R> outPort = new ReactiveOutPort<>();

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

    @Override
    public void subscribe(Subscriber<? super R> subscriber) {
        outPort.subscribe(subscriber);
    }

    /**
     *
     * @param item input data
     * @return processed data
     * @throws Throwable if something went wrong
     */
    protected abstract R whenNext(T item)  throws Throwable;

    protected void atComplete(Throwable cause) {
        super.atComplete(cause);
        if (cause == null) {
            outPort.onComplete();
        } else {
            outPort.onError(cause);
        }
    }

    /** processes one data item
     */
    @Override
    protected void run() {
        T item;
        try {
            item = inPort.remove();
        } catch (NoSuchElementException throwable) {
            Throwable thr = inPort.getCompletionException();
            atComplete(thr);
            return;
        }
        R res;
        try {
            res = whenNext(item);
        } catch (Throwable throwable) {
            atComplete(throwable);
            return;
        }
        if (res == null) {
            atComplete(null);
        } else {
            outPort.onNext(res);
            restart();
        }
    }
}

