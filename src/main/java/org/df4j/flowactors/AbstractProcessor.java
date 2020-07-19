package org.df4j.flowactors;

import java.util.concurrent.Flow;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends AbstractPublisher<R> implements Flow.Processor<T, R> {
    protected InPort<T> inPort = new InPort<>();

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
        try {
            T item = inPort.poll();
            if (inPort.isCompleted()) {
                Throwable thr = inPort.getCompletionException();
                if (thr == null) {
                    atComplete();
                } else {
                    atError(thr);
                }
            } else {
                atNext(item);
                R res= atNext(item);
                if (res==null) {
                    atComplete();
                } else if (outPort.onNext(res)) {
                    restart();
                } else {
                    atComplete();
                }
            }
        } catch (Throwable throwable) {
            inPort.onError(throwable);
        }
    }
}
