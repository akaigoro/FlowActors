package org.df4j.flowactors;

import java.util.logging.Logger;

public class ConsumerActor extends SubscriberActor<Long> {
    Logger logger = Logger.getLogger("consumer");
    final int delay;

    public ConsumerActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void atNext(Long item) {
        logger.info("  got:"+item);
    }

    @Override
    public void atComplete() {
        logger.info("  got: completed.");
    }

    @Override
    public void atError(Throwable throwable) {
        logger.info(" completed with error:"+throwable);
    }
}
