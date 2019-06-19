package com.terry.reactive.test.day02;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class GenericDelegateSub<T> implements Subscriber<T> {
    Subscriber sub;

    public GenericDelegateSub(Subscriber<? super T> sub) {
        this.sub = sub;
    }

    @Override
    public void onSubscribe(Subscription s) {
        // TODO Auto-generated method stub
        sub.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
        // TODO Auto-generated method stub
        sub.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        // TODO Auto-generated method stub
        sub.onError(t);
    }

    @Override
    public void onComplete() {
        // TODO Auto-generated method stub
        sub.onComplete();
    }
}
