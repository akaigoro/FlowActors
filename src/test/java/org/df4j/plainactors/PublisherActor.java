package org.df4j.plainactors;

import java.util.logging.Logger;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 */
public class PublisherActor extends AbstractPublisher<Long>{
    Logger logger = Logger.getLogger("producer");

    long cnt = 0;
    protected Long whenNext() {
        //Thread.sleep(delay);
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
