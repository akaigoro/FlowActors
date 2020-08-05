package org.df4j.plainactors;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<R> extends AbstractActor {
    public OutMessagePort<R> outPort;

    protected synchronized void complete() {
        super.complete();
        outPort.onComplete();
    }

    public OutMessagePort<R> getOutPort() {
        return outPort;
    }

    protected synchronized void completExceptionally(Throwable throwable) {
        super.completExceptionally(throwable);
        outPort.onError(throwable);
    }

    protected abstract R whenNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void turn() throws Throwable {
        R res = whenNext();
        if (res == null) {
            complete();
        } else {
            outPort.onNext(res);
        }
    }
}
