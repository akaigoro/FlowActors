package org.df4j.flowactors;

import java.util.concurrent.Flow;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends AbstractActor implements Flow.Processor<T, R> {
    private InPort<T> inPort = new InPort<>();
    private OutPort<R> outPort = new OutPort<>();

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

    protected void whenComplete() {
        outPort.onComplete();
    }
    protected void whenError(Throwable throwable) {
        outPort.onError(throwable);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        outPort.subscribe(subscriber);
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
            if (!inPort.isCompleted()) {
                T item = inPort.poll();
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
                    whenComplete();
                } else {
                    whenError(thr);
                }
            }
        } catch (Throwable throwable) {
            inPort.onError(throwable);
        }
    }
}
