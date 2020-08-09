package org.df4j.core.port;

import org.df4j.core.dataflow.Actor;

public class CompletablePort extends Actor.Port {
    protected volatile boolean completed = false;
    protected Throwable completionException = null;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param ready initial port state - port is not blocking the actor's execution
     */
    public CompletablePort(Actor parent, boolean ready) {
        super(parent, ready);
    }

    public CompletablePort(Actor parent) {
        super(parent, false);
    }

    public boolean isCompleted() {
        synchronized(parent) {
            return completed;
        }
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    protected  void _onComplete(Throwable throwable) {
        synchronized(parent) {
            if (completed) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            unblock();
        }
    }

    public void onComplete() {
        _onComplete(null);
    }

    public void onError(Throwable cause) {
        if (cause == null) {
            throw new NullPointerException();
        }
        _onComplete(cause);
    }
}
