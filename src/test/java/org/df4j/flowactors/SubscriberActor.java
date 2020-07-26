package org.df4j.flowactors;

import java.util.logging.Logger;

public class SubscriberActor extends AbstractSubscriber<Long> {
    Logger logger = Logger.getLogger("consumer");
    final int delay;

    public SubscriberActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void atNext(Long item) {
        if (Math.abs(item) < 100 || item%10 == 0) {
            logger.info("  got:"+item);
        }
    }

    @Override
    public void atComplete() {
        super.atComplete();
        logger.info("  got: completed.");
    }

    @Override
    public void atError(Throwable throwable) {
        logger.info(" completed with error:"+throwable);
    }
}
