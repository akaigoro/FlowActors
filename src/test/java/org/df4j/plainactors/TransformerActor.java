package org.df4j.plainactors;

import java.util.logging.Logger;

public class TransformerActor extends AbstractTransformer<Long,Long> {
    Logger logger = Logger.getLogger("processor");
    final int delay;

    public TransformerActor(int delay) {
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
        super.whenComplete();
        logger.info("  got: completed.");
    }

    @Override
    public void whenError(Throwable throwable) {
        super.whenError(throwable);
        logger.info(" completed with error:"+throwable);
    }
}
