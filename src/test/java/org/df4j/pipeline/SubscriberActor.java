package org.df4j.pipeline;

import org.df4j.plainactors.AbstractSubscriber;

import java.util.logging.Logger;

public class SubscriberActor extends AbstractSubscriber<Long> {
    AsyncSemaPort sema;
    Logger logger = Logger.getLogger("consumer");
    final int delay;

    public SubscriberActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void init() {
        inPort = new InPort<>();
    }

    @Override
    protected void whenNext(Long item) throws InterruptedException {
        sema.release(1);
        Thread.sleep(delay);
        if (Math.abs(item) < 100 || item%10 == 0) {
            logger.info("  got:"+item);
        }
    }

    @Override
    public void whenComplete() {
        super.complete();
        logger.info("  got: completed.");
    }

    @Override
    public void whenError(Throwable throwable) {
        logger.info(" completed with error:"+throwable);
    }
}
