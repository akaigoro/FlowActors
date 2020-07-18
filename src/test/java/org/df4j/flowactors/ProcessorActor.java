package org.df4j.flowactors;

import java.util.logging.Logger;

public class ProcessorActor extends AbstractProcessor<Long,Long> {
    Logger logger = Logger.getLogger("processor");
    final int delay;

    public ProcessorActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected Long atNext(Long item) {
        if (Math.abs(item) < 100 || item%10 == 0) {
            logger.info("  got:"+item);
        }
        return item;
    }

    @Override
    public void atComplete() {
        super.atComplete();
        logger.info("  got: completed.");
    }

    @Override
    public void atError(Throwable throwable) {
        super.atError(throwable);
        logger.info(" completed with error:"+throwable);
    }
}
