package org.df4j.reactivestreamstck;

import org.df4j.flowactors.SubscriberActor;
import  java.util.concurrent.Flow.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

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
    public Subscriber<Long> createFlowSubscriber() {
        return new SubscriberActor(0);
    }
}
