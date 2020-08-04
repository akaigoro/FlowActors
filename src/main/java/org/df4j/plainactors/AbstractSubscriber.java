package org.df4j.plainactors;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public abstract class AbstractSubscriber<T> extends AbstractActor {
    public InPort<T> inPort;

    protected AbstractSubscriber() {
        init();
    }

    protected void init() {
        inPort = new InPort<>();
    }

    protected abstract void whenNext(T item) throws Throwable;

    protected void whenComplete() {}

    protected void whenError(Throwable throwable) {}

    /** processes one data item
     */
    @Override
    protected void turn() throws Throwable {
        if (inPort.isCompletedExceptionally()) {
            Throwable completionException = inPort.getCompletionException();
            completExceptionally(completionException);
            whenError(completionException);
        } else  if (inPort.isCompleted()) {
            complete();
            whenComplete();
        } else {
            T item = inPort.poll();
            if (item==null) {
                throw new RuntimeException();
            }
            whenNext(item);
            restart();
        }
    }
}
