package org.df4j.reactiveactors;

import java.util.concurrent.Flow.Publisher;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends org.df4j.plainactors.AbstractPublisher<T> {

    protected void init() {
        outPort = new ReactiveOutPort<>(this);
    }
}
