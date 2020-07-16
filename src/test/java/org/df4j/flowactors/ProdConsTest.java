package org.df4j.flowactors;

import org.junit.Assert;
import org.junit.Test;

public class ProdConsTest {

    public void testProdCons(int cnt, int delay1, int delay2) throws Exception {
        ProducerActor prod = new ProducerActor(cnt, delay1);
        ConsumerActor cons = new ConsumerActor(delay2);
        prod.subscribe(cons);
        prod.start();
        cons.start();
        cons.join(Math.max(delay1, delay2)*cnt+1000);
        Assert.assertTrue(cons.isCompleted());
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


