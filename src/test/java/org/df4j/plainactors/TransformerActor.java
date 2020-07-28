package org.df4j.plainactors;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.NoSuchElementException;

/**
 * To make concrete processor, the method {@link TransformerActor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class TransformerActor<T, R> extends PlainActor implements Processor<T, R>, org.reactivestreams.Publisher<R> {
    protected InPort<T> inPort = new InPort<>();
    protected OutPort<R> outPort = new OutPort<>();

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
    protected abstract R atNext(T item)  throws Throwable;

    protected void atComplete() {
        super.atComplete();
        outPort.onComplete();
    }

    protected void atError(Throwable throwable) {
        outPort.onError(throwable);
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
            if (thr == null) {
                atComplete();
            } else {
                atError(thr);
            }
            return;
        }
        R res;
        try {
            res = atNext(item);
        } catch (Throwable throwable) {
            atError(throwable);
            return;
        }
        if (res == null) {
            atComplete();
        } else {
            outPort.onNext(res);
            restart();
        }
    }
}

