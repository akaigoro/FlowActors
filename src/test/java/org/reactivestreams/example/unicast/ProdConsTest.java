package org.reactivestreams.example.unicast;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class ProdConsTest {

    public void testProdCons(int cnt, int delay1, int delay2) throws Exception {
        Executor executor = ForkJoinPool.commonPool();
        Publisher<Integer> prod = new AsyncIterablePublisher<Integer>(new Iterable<Integer>() {
            @Override public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    private int at = 0;
                    @Override public boolean hasNext() { return at < cnt; }
                    @Override public Integer next(){
                        try {
                            Thread.sleep(delay1);
                        } catch (InterruptedException e) {
                        }
                        if (!hasNext()) return Collections.<Integer>emptyList().iterator().next();
                        else return at++;
                    }
                    @Override public void remove() { throw new UnsupportedOperationException(); }
                };
            }
        }, executor);
        final CountDownLatch latch = new CountDownLatch(1);
        Subscriber<Integer> cons = new AsyncSubscriber<Integer>(executor) {
            @Override protected boolean whenNext(final Integer element) {
                return true;
            }

            @Override protected void whenComplete() {
                latch.countDown();
            }
        };
        prod.subscribe(cons);
        latch.await(400, TimeUnit.MILLISECONDS);
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
    public void testVerySlowProdCons() throws Exception {
        testProdCons(5,100000, 0);
    }

    @Test
    public void testProdSlowCons() throws Exception {
        testProdCons(5,0, 50);
    }
}


