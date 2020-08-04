package org.df4j.reactivestreamstck;

import org.df4j.reactiveactors.ProcessorActor;
import org.df4j.reactiveactors.PublisherActor;
import org.reactivestreams.tck.TestEnvironment;

import  java.util.concurrent.Flow.Publisher;
import  java.util.concurrent.Flow.Subscriber;

public class ProcessorVerificationTest extends org.reactivestreams.tck.flow.FlowPublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public ProcessorVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        PublisherActor publisher = new PublisherActor(elements);
        ProcessorActor processor = new ProcessorActor(0);
        publisher.outPort.subscribe(processor.inPort);
        publisher.start();
        processor.start();
        return processor.outPort;
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        MyFailedPublisherActor actor = new MyFailedPublisherActor();
        actor.start();
        return actor;
    }

    private static class MyFailedPublisherActor extends PublisherActor implements Publisher<Long>{

        public MyFailedPublisherActor() {
            super(0);
        }

        @Override
        public void subscribe(Subscriber<? super Long> subscriber) {
            outPort.subscribe(subscriber);
            subscriber.onError(new IllegalStateException());
        }

        @Override
        protected Long whenNext() {
            throw new RuntimeException();
        }
    }
}
