package org.df4j.reactiveactors;

public abstract class AbstractSubscriber<T> extends org.df4j.plainactors.AbstractSubscriber<T> {

    protected void init() {
        inPort = new ReactiveInPort<>(this);
    }
}
