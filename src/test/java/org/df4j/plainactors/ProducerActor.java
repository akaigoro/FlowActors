package org.df4j.plainactors;

import java.util.logging.Logger;

public class ProducerActor extends AbstractProducer<Long> {
    AsyncSemaPort sema = new AsyncSemaPort(1);
    Logger logger = Logger.getLogger("producer");
    final int delay;
    long cnt;

    public ProducerActor(long cnt, int delay) {
        this.cnt = cnt;
        this.delay = delay;
    }

    @Override
    protected Long whenNext() throws Throwable {
        sema.aquire(1);
        Thread.sleep(delay);
        if (cnt == 0) {
            logger.info("sent: completed");
            return null;
        } else {
            if (Math.abs(cnt) < 100 || cnt%10 == 0) {
                logger.info("sent:" + cnt);
            }
            return cnt--;
        }
    }
}
