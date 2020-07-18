package org.df4j.flowactors;

import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Logger;

public class ProducerActor extends PublisherActor<Long> {
    Logger logger = Logger.getLogger("producer");
    final int delay;
    long cnt;

    public ProducerActor(long cnt, int delay) {
        this.cnt = cnt;
        this.delay = delay;
    }

    public ProducerActor(long cnt) {
        this.cnt = cnt;
        this.delay = 0;
    }

    @Override
    protected Long atNext() throws Throwable {
        Thread.sleep(delay);
        if (cnt == 0) {
            logger.info("sent: completed");
            return null;
        } else {
            logger.info("sent:"+cnt);
            return cnt--;
        }
    }
}
