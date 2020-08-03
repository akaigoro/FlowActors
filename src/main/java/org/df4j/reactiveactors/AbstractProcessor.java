package org.df4j.reactiveactors;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Processor;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends org.df4j.plainactors.AbstractProcessor<T,R>
        implements Processor<T, R>, Flow.Publisher<R>
{

    @Override
    protected void init() {
        inPort = new ReactiveInPort<>();
        outPort = new ReactiveOutPort<>();
    }
}

