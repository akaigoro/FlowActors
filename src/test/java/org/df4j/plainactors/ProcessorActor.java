package org.df4j.plainactors;

import java.util.logging.Logger;

public class ProcessorActor extends AbstractProcessor<Long,Long> {
    Logger logger = Logger.getLogger("processor");
    final int delay;

    public ProcessorActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected Long whenNext(Long item) {
        if (Math.abs(item) < 100 || item%10 == 0) {
            logger.info("  got:"+item);
        }
        return item;
    }

    @Override
    protected void whenComplete() {
        logger.info("  got: completed.");
    }

    @Override
    protected void whenError(Throwable throwable) {
        logger.info(" completed with error:"+throwable);
    }
}
