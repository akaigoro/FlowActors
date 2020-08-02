package org.df4j.reactiveactors;

import java.util.logging.Logger;

public class SubscriberActor extends AbstractSubscriber<Long> {
    Logger logger = Logger.getLogger("consumer");
    final int delay;

    public SubscriberActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void whenNext(Long item) throws InterruptedException {
        Thread.sleep(delay);
        if (Math.abs(item) < 100 || item%10 == 0) {
            logger.info("  got:"+item);
        }
    }

    @Override
    public void whenComplete() {
        logger.info("  got: completed.");
    }

    @Override
    public void whenError(Throwable throwable) {
        logger.info(" completed with error:"+throwable);
    }
}
