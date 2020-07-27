package org.df4j.reactivestreamstck;

import org.df4j.flowactors.SubscriberActor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

import org.reactivestreams.Publisher;

public class SubscriberBlackboxVerificationTest extends org.reactivestreams.tck.SubscriberBlackboxVerification<Long> {
    static final  int defaultTimeout = 400;

    protected SubscriberBlackboxVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Long createElement(int i) {
        return (long)i;
    }

    @Override
    public Subscriber<Long> createSubscriber() {
        return new SubscriberActor(0);
    }
}
