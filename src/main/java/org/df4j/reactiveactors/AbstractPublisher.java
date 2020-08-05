package org.df4j.reactiveactors;

import org.df4j.plainactors.AbstractProducer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends AbstractProducer<T> implements Publisher<T> {

    protected void init() {
        outPort = new ReactiveOutPort<>(this);
    }

    public ReactiveOutPort<T> getOutPort() {
        return (ReactiveOutPort<T>) outPort;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        getOutPort().subscribe(subscriber);
    }
}
