package org.df4j.plainactors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.logging.Logger;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 */
public class ProducerActor extends PlainActor implements Publisher<Long>{
    Logger logger = Logger.getLogger("producer");
    protected OutPort<Long> outPort = new OutPort<>();

    protected void atComplete() {
        super.atComplete();
        outPort.onComplete();
    }
    protected void atError(Throwable throwable) {
        outPort.onError(throwable);
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        outPort.subscribe(subscriber);
    }

    long cnt = 0;
    protected Long atNext() {
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

    /** generates one data item
     */
    @Override
    protected void run() {
        try {
            Long res = atNext();
            if (res==null) {
                atComplete();
            } else {
                outPort.onNext(res);
                restart();
            }
        } catch (Throwable throwable) {
            atError(throwable);
        }
    }
}
