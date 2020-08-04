package org.df4j.plainactors;

import java.util.concurrent.Flow.Publisher;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends AbstractActor {
    public OutPort<T> outPort;

    protected void init() {
        outPort = new OutPort<>();
    }

    protected synchronized void complete() {
        super.complete();
        outPort.onComplete();
    }

    protected synchronized void completExceptionally(Throwable throwable) {
        super.completExceptionally(throwable);
        outPort.onError(throwable);
    }

    protected abstract T whenNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void turn() throws Throwable {
        T res = whenNext();
        if (res == null) {
            complete();
        } else {
            outPort.onNext(res);
        }
    }
}
