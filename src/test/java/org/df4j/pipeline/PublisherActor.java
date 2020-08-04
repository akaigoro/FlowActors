package org.df4j.pipeline;

import org.df4j.plainactors.AbstractPublisher;

import java.util.logging.Logger;

public class PublisherActor extends AbstractPublisher<Long> {
    AsyncSemaPort sema = new AsyncSemaPort(1);
    Logger logger = Logger.getLogger("producer");
    final int delay;
    long cnt;

    public PublisherActor(long cnt, int delay) {
        this.cnt = cnt;
        this.delay = delay;
    }

    public PublisherActor(long cnt) {
        this(cnt,0);
    }

    @Override
    protected void init() {
        outPort = new OutPort<>();
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
