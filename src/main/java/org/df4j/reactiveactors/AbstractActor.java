package org.df4j.reactiveactors;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public abstract class AbstractActor extends org.df4j.plainactors.AbstractActor {

    public class ReactiveInPort<T> extends InPort<T> {

        @Override
        public void onSubscribe(Subscription subscription) {
            super.onSubscribe(subscription);
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

    public class ReactiveOutPort<T> extends OutPort<T> {
        AsyncSemaPort sema = new AsyncSemaPort();

        @Override
        public void request(long n) {
            Subscriber<? super T> subscriber;
            synchronized (AbstractActor.this) {
                subscriber = subscriberPort.current();
                if (subscriber == null) {
                    return;
                }
                try {
                    sema.release(n);
                } catch (IllegalArgumentException e) {
                    subscriber.onError(e);
                }
            }
        }

        public void onNext(T item) {
            Subscriber<? super T> subscriber;
            synchronized (AbstractActor.this) {
                subscriber = subscriberPort.current();
                if (subscriber == null) {
                    return;
                }
                try {
                    sema.aquire();
                } catch (IllegalArgumentException e) {
                    subscriber.onError(e);
                    return;
                }
            }
            subscriber.onNext(item);
        }
    }
}
