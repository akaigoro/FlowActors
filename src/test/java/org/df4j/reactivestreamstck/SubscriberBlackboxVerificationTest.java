package org.df4j.reactivestreamstck;

import org.df4j.flowactors.ConsumerActor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

import java.util.concurrent.Flow;

public class SubscriberBlackboxVerificationTest extends org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification<Long> {
    static final  int defaultTimeout = 400;

    protected SubscriberBlackboxVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Long createElement(int i) {
        return (long)i;
    }

    @Override
    public Flow.Subscriber<Long> createFlowSubscriber() {
        return new ConsumerActor(0);
    }
}
