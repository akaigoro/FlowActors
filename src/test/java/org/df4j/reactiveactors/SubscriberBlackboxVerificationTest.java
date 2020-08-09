package org.df4j.reactiveactors;

import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

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
        return new SubscriberActor(0).getInPort();
    }
}
