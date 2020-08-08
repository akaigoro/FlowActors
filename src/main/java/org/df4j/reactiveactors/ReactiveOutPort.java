package org.df4j.reactiveactors;

import org.df4j.plainactors.AbstractActor;
import org.df4j.plainactors.OutMessagePort;
import org.reactivestreams.*;

public class ReactiveOutPort<T> implements Publisher<T>, Subscription, OutMessagePort<T> {
    protected AbstractActor.InPort<Subscriber<? super T>> subscriber;
    protected AbstractActor.AsyncSemaPort sema;

    public ReactiveOutPort(AbstractActor actor) {
        subscriber = actor.new InPort<>();
        sema = actor.new AsyncSemaPort();
    }

    @Override
    public synchronized void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }
        if (this.subscriber.isFull()) {
            subscriber.onError(new IllegalStateException());
            return;
        }
        this.subscriber.onNext(subscriber);
        subscriber.onSubscribe(this);
    }

    @Override
    public synchronized void request(long n) {
        if (subscriber.isEmpty()) {
            return; // this spec requirement
        }
        if (n <= 0) {
            subscriber.current().onError(new IllegalArgumentException());
            return;
        }
        sema.release(n);
    }

    public synchronized void onNext(T item) {
        Subscriber<? super T> subscriber = this.subscriber.current();
        if (subscriber == null) {
            throw  new IllegalStateException();
        }
        sema.aquire();
        subscriber.onNext(item);
    }

    @Override
    public synchronized void cancel() {
        Subscriber<? super T> subscriber = this.subscriber.current();
        if (subscriber == null) {
            return;
        }
        this.subscriber.remove();
        this.sema.setPermissions(0);
    }

    public synchronized void onComplete() {
        Subscriber<? super T> subscriber = this.subscriber.poll();
        if (subscriber == null) {
            return;
        }
        subscriber.onComplete();
    }

    public synchronized void onError(Throwable throwable) {
        Subscriber<? super T> subscriber = this.subscriber.poll();
        if (subscriber == null) {
            return;
        }
        subscriber.onError(throwable);
    }
}
