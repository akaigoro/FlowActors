package org.df4j.reactiveactors;

import java.util.concurrent.Flow.Subscriber;

public abstract class AbstractSubscriber<T> extends org.df4j.plainactors.AbstractSubscriber<T> {

    protected void init() {
        inPort = new ReactiveInPort<>(this);
    }
}
