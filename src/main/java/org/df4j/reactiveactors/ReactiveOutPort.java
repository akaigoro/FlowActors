package org.df4j.reactiveactors;

import org.df4j.plainactors.AbstractActor;

import java.util.concurrent.Flow;

public class ReactiveOutPort<T> extends AbstractActor.OutPort<T> {
    private final AbstractActor actor;
    AbstractActor.AsyncSemaPort sema;

    public ReactiveOutPort(AbstractActor actor) {
        actor.super();
        this.actor = actor;
        sema = actor.new AsyncSemaPort();
    }

    @Override
    public void request(long n) {
        Flow.Subscriber<? super T> subscriber = subscriberPort.current();
        if (subscriber == null) {
            return;
        }
        sema.release(n);
    }

    public synchronized void onNext(T item) {
        Flow.Subscriber<? super T> subscriber = subscriberPort.current();
        if (subscriber == null) {
            return;
        }
        sema.aquire();
        subscriber.onNext(item);
    }
}
