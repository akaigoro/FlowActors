package org.df4j.reactiveactors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

public class ProcessorVerificationTest extends org.reactivestreams.tck.PublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public ProcessorVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        PublisherActor publisher = new PublisherActor(elements);
        ProcessorActor processor = new ProcessorActor(0);
        publisher.subscribe(processor);
        publisher.start();
        processor.start();
        return processor;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        MyFailedPublisherActor actor = new MyFailedPublisherActor();
        actor.start();
        return actor;
    }

    private static class MyFailedPublisherActor extends PublisherActor{

        public MyFailedPublisherActor() {
            super(0);
        }

        @Override
        public void subscribe(Subscriber<? super Long> subscriber) {
            super.subscribe(subscriber);
            subscriber.onError(new IllegalStateException());
        }

        @Override
        protected Long whenNext() {
            throw new RuntimeException();
        }
    }
}
