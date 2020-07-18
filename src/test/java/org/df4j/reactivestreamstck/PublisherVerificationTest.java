package org.df4j.reactivestreamstck;

import org.df4j.flowactors.PublisherActor;
import org.reactivestreams.tck.TestEnvironment;

import java.util.concurrent.Flow;

public class PublisherVerificationTest extends org.reactivestreams.tck.flow.FlowPublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public PublisherVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        PublisherActor publisher = new PublisherActor(elements);
        publisher.start();
        return publisher;
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        MyFailedPublisherActor publisher = new MyFailedPublisherActor();
        publisher.start();
        return publisher;
    }

    private static class MyFailedPublisherActor extends PublisherActor {

        public MyFailedPublisherActor() {
            super(0);
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Long> subscriber) {
            super.subscribe(subscriber);
            subscriber.onError(new IllegalStateException());
        }

        @Override
        protected Long atNext() {
            throw new RuntimeException();
        }
    }
}
