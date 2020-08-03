package org.df4j.pipeline;

import org.df4j.plainactors.AbstractProcessor;

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
    public void whenComplete() {
        super.complete();
        logger.info("  got: completed.");
    }

    @Override
    public void completExceptionally(Throwable throwable) {
        super.completExceptionally(throwable);
        logger.info(" completed with error:"+throwable);
    }
}
