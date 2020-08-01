package org.df4j.flowactors;

import java.util.NoSuchElementException;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends AbstractPublisher<R> implements Processor<T, R> {
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

    @Override
    protected R atNext() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param item input data
     * @return processed data
     * @throws Throwable if something went wrong
     */
    protected abstract R atNext(T item)  throws Throwable;

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

