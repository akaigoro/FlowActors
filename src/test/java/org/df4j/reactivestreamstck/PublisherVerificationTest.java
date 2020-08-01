package org.df4j.reactivestreamstck;

import org.df4j.flowactors.PublisherActor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

public class PublisherVerificationTest extends org.reactivestreams.tck.PublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public PublisherVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        PublisherActor publisher = new PublisherActor(elements);
        publisher.start();
        return publisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        MyFailedPublisherActor publisher = new MyFailedPublisherActor();
        publisher.start();
        return publisher;
    }

    private static class MyFailedPublisherActor extends PublisherActor {

        public MyFailedPublisherActor() {
            super(0);
        }

        @Override
        public void subscribe(Subscriber<? super Long> subscriber) {
            super.subscribe(subscriber);
            subscriber.onError(new IllegalStateException());
        }

        @Override
        protected Long atNext() {
            throw new RuntimeException();
        }
    }
}
