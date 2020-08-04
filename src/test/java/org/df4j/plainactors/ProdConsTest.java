package org.df4j.plainactors;

import org.junit.Assert;
import org.testng.annotations.Test;

public class ProdConsTest {

    public void noFeedBack(int cnt, int delay1, int delay2) throws Exception {
        PublisherActor prod = new PublisherActor(cnt, delay1);
        SubscriberActor cons = new SubscriberActor(delay2);
        prod.outPort.subscribe(cons.inPort);
        prod.start();
        cons.start();
        cons.join(Math.max(delay1, delay2)*cnt+400);
        boolean completed = cons.isCompleted();
        Assert.assertTrue(completed);
    }

    @Test
    public void testComplete() throws Exception {
        noFeedBack(0,0, 0);
    }

    @Test(expectedExceptions = {java.lang.Exception.class})
    public void testProdSlowCons() throws Exception {
        noFeedBack(5,0, 50);
    }
}


