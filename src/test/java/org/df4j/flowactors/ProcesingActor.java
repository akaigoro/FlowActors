package org.df4j.flowactors;

import java.util.logging.Logger;

public class ProcesingActor extends ProcessorActor<Long,Long> {
    Logger logger = Logger.getLogger("consumer");
    final int delay;

    public ProcesingActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected Long atNext(Long item) {
        logger.info("  got:"+item);
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
