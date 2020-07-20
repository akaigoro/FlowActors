package org.df4j.reactivestreamstck;

import org.df4j.flowactors.ProcessorActor;
import org.df4j.flowactors.PublisherActor;
import org.reactivestreams.tck.TestEnvironment;

import java.util.concurrent.Flow;

public class ProcessorVerificationTest extends org.reactivestreams.tck.flow.FlowPublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public ProcessorVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        PublisherActor publisher = new PublisherActor(elements);
        ProcessorActor processor = new ProcessorActor(0);
        publisher.subscribe(processor);
        publisher.start();
        processor.start();
        return processor;
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        MyFailedPublisherActor actor = new MyFailedPublisherActor();
        actor.start();
        return actor;
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