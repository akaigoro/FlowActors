package org.df4j.reactivestreamstck;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.OutFlow;
import org.reactivestreams.tck.TestEnvironment;

import java.util.concurrent.Flow;
import java.util.logging.Level;

public class PublisherVerificationTest extends org.reactivestreams.tck.flow.FlowPublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public PublisherVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        LoggingPublisherActor publisher = new LoggingPublisherActor(elements);
        publisher.start();
        return publisher;
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        MyPublisherActor publisher = new MyPublisherActor();
        publisher.start();
        return publisher.out;
    }

    // todo remove
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }

    static class LoggingPublisherActor extends Actor implements Flow.Publisher<Long> {
        protected final Logger logger = new Logger(this);
        final int delay;
        public OutFlow<Long> out;
        public long cnt;

        {
            setLogLevel(Level.OFF);
        }

        public LoggingPublisherActor(long elements) {
            this(elements, 0);
            setLogLevel(Level.OFF);
        }

        public LoggingPublisherActor(long cnt, int delay) {
            this(cnt, delay, OutFlow.DEFAULT_CAPACITY);
        }

        public LoggingPublisherActor(long cnt, int delay, int capacity) {
            out = new OutFlow<>(this, capacity);
            this.cnt = cnt;
            this.delay = delay;
            logger.info("PublisherActor: cnt = " + cnt);
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Long> s) {
            logger.info("PublisherActor.subscribe:");
            out.subscribe(new ProxySubscriber(s));
        }

        public void setLogLevel(Level off) {
            logger.setLevel(off);
        }

        @Override
        protected void runAction() throws Throwable {
            if (cnt > 0) {
                logger.info("PublisherActor.onNext(" + cnt+")");
                out.onNext(cnt);
                cnt--;
                Thread.sleep(delay);
            } else {
                logger.info("PublisherActor.onComplete");
                out.onComplete();
                complete();
            }
        }

        private class ProxySubscription implements Flow.Subscription {
            private Flow.Subscription subscription;

            public ProxySubscription(Flow.Subscription subscription) {
                this.subscription = subscription;
            }

            @Override
            public void request(long n) {
                logger.info("    Subscription.request:"+n);
                subscription.request(n);
            }

            @Override
            public void cancel() {
                logger.info("    Subscription.cancel:");
                subscription.cancel();
            }
        }

        class ProxySubscriber implements Flow.Subscriber<Long> {
            private final Flow.Subscriber<? super Long> sub;

            public ProxySubscriber(Flow.Subscriber<? super Long> s) {
                sub = s;
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                logger.info("        Subscriber.onSubscribe");
                ProxySubscription proxy = new ProxySubscription(subscription);
                sub.onSubscribe(proxy);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("        Subscriber.onError:"+t);
                sub.onError(t);
            }

            @Override
            public void onComplete() {
                logger.info("        Subscriber.onComplete");
//                new Exception().printStackTrace();
                sub.onComplete();
            }

            @Override
            public void onNext(Long t) {
                logger.info("        Subscriber.onNext:"+t);
                sub.onNext(t);
            }
        }
    }

    private static class MyPublisherActor extends Actor {
        public OutFlow<Long> out = new OutFlow<>(this);

        @Override
        protected void runAction() {
            out.onError(new RuntimeException());
            complete();
        }
    }
}
