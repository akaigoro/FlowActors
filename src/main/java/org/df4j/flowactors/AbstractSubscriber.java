package org.df4j.flowactors;

import java.util.concurrent.Flow;

public abstract class AbstractSubscriber<T> extends AbstractActor implements Flow.Subscriber<T> {
    private InPort<T> inPort = new InPort<>();
    private Flow.Subscription subscription;
    private boolean completeSignalled;
    private Throwable completionException = null;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
       synchronized (this) {
           if (this.subscription != null) {
               subscription.cancel();
               return;
           }
           this.subscription = subscription;
       }
       subscription.request(1);
    }

    public synchronized void  onNext(T item) {
        if (completeSignalled) {
            return;
        }
        if (item == null) {
            throw new NullPointerException();
        }
        inPort.unBlock();
    }

    public T poll() {
        T res;
        synchronized (this) {
            if (subscription == null) {
                throw new IllegalStateException();
            }
            res = inPort.poll();
            if (completeSignalled) {
                state =  State.COMPLETED;
                this.notifyAll();
            } else {
                subscription.request(1);
            }
        }
        return res;
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        if (completeSignalled) {
            return;
        }
        if (throwable == null) {
            throw new NullPointerException();
        }
        completeSignalled = true;
        completionException = throwable;
        inPort.unBlock();
    }

    @Override
    public synchronized void onComplete() {
        if (completeSignalled) {
            return;
        }
        completeSignalled = true;
        inPort.unBlock();
    }

    protected abstract void atNext(T item) throws Throwable;
    protected void atComplete() {}
    protected void atError(Throwable throwable) {
        throwable.printStackTrace();
    }

    /** processes one data item
     */
    @Override
    protected void run() {
        try {
            T item = poll();
            if (!isCompleted()) {
                atNext(item);
                restart();
            } else {
                if (completionException == null) {
                    atComplete();
                } else {
                    atError(completionException);
                }
            }
        } catch (Throwable throwable) {
            onError(throwable);
        }
    }
}
