package org.df4j.reactiveactors;

import org.df4j.plainactors.AbstractActor;
import org.df4j.plainactors.OutMessagePort;
import org.reactivestreams.*;

public class ReactiveOutPort<T> implements Publisher<T>, Subscription, OutMessagePort<T> {
    protected Subscriber<? super T> subscriber;
    AbstractActor.AsyncSemaPort sema;

    public ReactiveOutPort(AbstractActor actor) {
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
    public synchronized void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            subscriber.onError(new NullPointerException());
            return;
        }
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
    }

    @Override
    public synchronized void cancel() {
        Subscriber<? super T> subscriber = this.subscriber;
        if (subscriber == null) {
            return;
        }
        this.subscriber= null;
    }

    public synchronized void onComplete() {
        Subscriber<? super T> subscriber = this.subscriber;
        if (subscriber == null) {
            return;
        }
        this.subscriber= null;
        subscriber.onComplete();
    }

    public synchronized void onError(Throwable throwable) {
        Subscriber<? super T> subscriber = this.subscriber;
        if (subscriber == null) {
            return;
        }
        this.subscriber= null;
        subscriber.onError(throwable);
    }
}
