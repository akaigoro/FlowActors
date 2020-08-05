package org.df4j.reactiveactors;

import org.df4j.plainactors.AbstractActor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ReactiveInPort<T> extends AbstractActor.InPort<T> implements Subscriber<T> {
    protected Subscription subscription;

    public ReactiveInPort(AbstractActor actor) {
        actor.super();
    }

    @Override
    public synchronized void onSubscribe(Subscription subscription) {
        if (this.subscription != null) {
            subscription.cancel();
            return;
        }
        this.subscription = subscription;
        request(1);
    }

    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public T poll() {
        T res = super.poll();
        request(1);
        return res;
    }

    @Override
    public T remove() {
        T res = super.remove();
        request(1);
        return res;
    }
}
