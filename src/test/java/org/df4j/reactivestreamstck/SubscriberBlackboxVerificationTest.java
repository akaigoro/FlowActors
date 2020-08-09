package org.df4j.reactivestreamstck;

import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

import java.util.concurrent.Flow;

public class SubscriberBlackboxVerificationTest extends org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification {
    static final  int defaultTimeout = 400;

    protected SubscriberBlackboxVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Flow.Subscriber createFlowSubscriber() {
        return new SubscriberActor(0).inp;
    }

    @Override
    public Object createElement(int element) {
        return element;
    }
}
