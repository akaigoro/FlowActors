package org.df4j.plainactors;

public interface OutMessagePort<T> {
    void onNext(T message);
    void onComplete();
    void onError(Throwable ex);
}
