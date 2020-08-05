package org.df4j.reactiveactors;

import org.df4j.plainactors.OutMessagePort;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends org.df4j.plainactors.AbstractPublisher<T> implements Flow.Publisher<T> {

    protected void init() {
        outPort = new ReactiveOutPort<>(this);
    }

    @Override
    public ReactiveOutPort<T> getOutPort() {
        return (ReactiveOutPort<T>) super.getOutPort();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        getOutPort().subscribe(subscriber);
    }
}
