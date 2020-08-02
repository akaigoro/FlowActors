package org.df4j.reactiveactors;

import org.junit.Assert;
import org.testng.annotations.Test;

public class ProdConsTest {

    public void testProdCons(int cnt, int delay1, int delay2) throws Exception {
        PublisherActor prod = new PublisherActor(cnt, delay1);
        SubscriberActor cons = new SubscriberActor(delay2);
        prod.subscribe(cons);
        prod.start();
        cons.start();
        cons.join(Math.max(delay1, delay2)*cnt+400);
        boolean completed = cons.isCompleted();
        Assert.assertTrue(completed);
    }

    @Test
    public void testComplete() throws Exception {
        testProdCons(0,0, 0);
    }

    @Test
    public void testFastProdCons() throws Exception {
        testProdCons(5,0, 0);
    }

    @Test
    public void testSlowProdCons() throws Exception {
        testProdCons(5,100, 0);
    }

    @Test
    public void testProdSlowCons() throws Exception {
        testProdCons(5,0, 50);
    }
}


