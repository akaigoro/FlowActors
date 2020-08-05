package org.df4j.plainactors;

/**
 * minimalistic implementation.
 * Only one subscriber can subscribe.
 * @param <R> type of produced data
 */
public abstract class AbstractProducer<R> extends AbstractActor {
    public OutMessagePort<R> outPort;

    protected synchronized void whenComplete() {
        super.whenComplete();
        outPort.onComplete();
    }

    protected synchronized void whenError(Throwable throwable) {
        super.whenError(throwable);
        outPort.onError(throwable);
    }

    protected abstract R whenNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void turn() throws Throwable {
        R res = whenNext();
        if (res == null) {
            whenComplete();
        } else {
            outPort.onNext(res);
        }
    }
}
