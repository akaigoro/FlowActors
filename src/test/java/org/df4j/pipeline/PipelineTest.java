package org.df4j.pipeline;

import org.junit.Assert;
import org.testng.annotations.Test;

public class PipelineTest {

    public void testProdCons(int cnt, int delay1, int delay2) throws Exception {
        PublisherActor prod = new PublisherActor(cnt, delay1);
        ProcessorActor proc1 = new ProcessorActor(delay2);
        ProcessorActor proc2 = new ProcessorActor(delay2);
        SubscriberActor cons = new SubscriberActor(delay2);
        cons.sema=prod.sema;
        prod.subscribe(proc1);
        proc1.subscribe(proc2);
        proc2.subscribe(cons);
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


