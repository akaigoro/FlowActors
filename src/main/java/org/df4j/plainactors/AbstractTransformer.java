package org.df4j.plainactors;

/**
 * To make concrete processor, the method {@link AbstractTransformer##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractTransformer<T, R> extends AbstractActor {
    public InPort<T> inPort;
    public OutMessagePort<R> outPort;

    protected void init() {
        inPort = new InPort<>();
    }

    public InPort<T> getInPort() {
        return inPort;
    }

    public OutMessagePort<R> getOutPort() {
        return outPort;
    }

    protected synchronized void whenComplete() {
        super.whenComplete();
        outPort.onComplete();
    }

    protected synchronized void whenError(Throwable throwable) {
        super.whenError(throwable);
        outPort.onError(throwable);
    }

    /**
     *
     * @param item input data
     * @return processed data
     * @throws Throwable if something went wrong
     */
    protected abstract R whenNext(T item)  throws Throwable;

    /** processes one data item
     */
    @Override
    protected void turn() throws Throwable {
        if (inPort.isCompletedExceptionally()) {
            Throwable completionException = inPort.getCompletionException();
            whenError(completionException);
        } else  if (inPort.isCompleted()) {
            whenComplete();
        } else {
            T item = inPort.poll();
            if (item==null) {
                throw new RuntimeException();
            }
            R res = whenNext(item);
            if (res == null) {
                whenComplete();
            } else {
                outPort.onNext(res);
            }
        }
    }
}

