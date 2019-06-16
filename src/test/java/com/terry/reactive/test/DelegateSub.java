package com.terry.reactive.test;

import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class DelegateSub implements Subscriber<Integer> {
  Subscriber sub;
  public DelegateSub(Subscriber sub) {
    this.sub = sub;
  }
  @Override
  public void onSubscribe(Subscription s) {
    // TODO Auto-generated method stub
    sub.onSubscribe(s);
  }

  @Override
  public void onNext(Integer i) {
    // TODO Auto-generated method stub
    sub.onNext(i);
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
