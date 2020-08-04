package org.df4j.reactiveactors;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends org.df4j.plainactors.AbstractProcessor<T,R> {

    protected void init() {
        inPort = new ReactiveInPort<>(this);
        outPort = new ReactiveOutPort<>(this);
    }
}

