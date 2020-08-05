package org.df4j.plainactors;

import org.junit.Assert;
import org.testng.annotations.Test;

public class PipelineTest {

    public void testProdCons(int cnt, int delay1, int delay2) throws Exception {
        ProducerActor prod = new ProducerActor(cnt, delay1);
        TransformerActor proc1 = new TransformerActor(delay2);
        TransformerActor proc2 = new TransformerActor(delay2);
        ConsumerActor cons = new ConsumerActor(delay2);
        prod.outPort = proc1.inPort;
        proc1.outPort = proc2.inPort;
        proc2.outPort = cons.inPort;
        cons.sema=prod.sema;
        prod.start();
        proc1.start();
        proc2.start();
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


