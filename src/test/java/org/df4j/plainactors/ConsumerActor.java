package org.df4j.plainactors;

import java.util.logging.Logger;

public class ConsumerActor extends AbstractConsumer<Long> {
    AsyncSemaPort sema;
    Logger logger = Logger.getLogger("consumer");
    final int delay;

    public ConsumerActor(int delay) {
        this.delay = delay;
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
        super.whenComplete();
        logger.info("  got: completed.");
    }

    @Override
    public void whenError(Throwable throwable) {
        super.whenError(throwable);
        logger.info(" completed with error:"+throwable);
    }
}
