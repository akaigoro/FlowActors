package org.df4j.reactiveactors;

import org.df4j.plainactors.AbstractActor;
import org.df4j.plainactors.OutMessagePort;

import java.util.concurrent.Flow;

public class ReactiveOutPort<T> implements Flow.Publisher<T>, Flow.Subscription, OutMessagePort<T> {
    private final AbstractActor actor;
    protected Flow.Subscriber<? super T> subscriber;
    AbstractActor.AsyncSemaPort sema;

    public ReactiveOutPort(AbstractActor actor) {
        this.actor = actor;
        sema = actor.new AsyncSemaPort();
    }

    @Override
    public synchronized void request(long n) {
        if (subscriber == null) {
            return;
        }
        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException());
            return;
        }
        sema.release(n);
    }

    public synchronized void onNext(T item) {
        if (subscriber == null) {
            return;
        }
        sema.aquire();
        subscriber.onNext(item);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            subscriber.onError(new NullPointerException());
            return;
        }
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
    }

    @Override
    public synchronized void cancel() {
        Flow.Subscriber<? super T> subscriber = this.subscriber;
        if (subscriber == null) {
            return;
        }
        this.subscriber= null;
    }

    public synchronized void onComplete() {
        Flow.Subscriber<? super T> subscriber = this.subscriber;
        if (subscriber == null) {
            return;
        }
        this.subscriber= null;
        subscriber.onComplete();
    }

    public void onError(Throwable throwable) {
        Flow.Subscriber<? super T> subscriber = this.subscriber;
        if (subscriber == null) {
            return;
        }
        this.subscriber= null;
        subscriber.onError(throwable);
    }
}
