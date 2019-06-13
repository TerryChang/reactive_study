package com.terry.reactive.test;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Arrays;
import java.util.Iterator;

public class Pubsub {

    // Publisher <-- Observable
    // Subscriber <-- Observer
    // Subscriber는 다음의 메소드를 호출한다
    // onSubscribe -> onNext* -> (onError | onComplete)
    // onSubscribe 메소드를 먼저 호출한뒤 onNext 메소드를 0번 이상 호출하고 마지막으로 onComplete 메소드나 onError 메소드를 호출해야 한다

    @Test
    public void onPublisherTest() {
        Iterable<Integer> iter = Arrays.asList(1,2,3,4,5);

        Publisher publisher = new Publisher() {
            @Override
            public void subscribe(Subscriber subscriber) {
                Iterator iterator = iter.iterator();

                subscriber.onSubscribe(new Subscription() {
                    // 이 메소드는 제공자(Publisher)와 구독자(Subscriber)의 처리속도를 맞추기 위해 제공된다.
                    // 구독자가 제공자보다 처리속도가 느릴경우 제공자는 구독자에게 전체 처리되어야 할 데이터를 한꺼번에 보내는게 아니라 나누어서 보내라고 요청할 수 있다
                    // 구독자에게 보내줘야 할 데이터 갯수가 지정되어서 보내어지게 된다
                    // 무한대로 보내어지도록 할 경우 Long.MAX_VALUE로 파라미터에 넣어주면 된다
                    // 이 메소드는 나에게 몇개를 보내달라고 요청을 하는 것이지 실제로 무슨 응답을 해달라는 것은 아니다(메소드의 return 타입이 void이다)
                    // 구독자가 request 메소드를 통해 10개를 보내달라고 요청해도 제공자는 이 request 메소드가 실행되는 시점에 아직 아무것도 안하는 상태일수도 있다
                    @Override
                    public void request(long n) {
                        while(n-- > 0) {
                            if (iterator.hasNext()) {
                                subscriber.onNext(iterator.next());
                            } else {
                                subscriber.onComplete();
                                break;
                            }
                        }
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        };

        Subscriber<Integer> subscriber = new Subscriber<Integer>() {
            Subscription subscription;
            @Override
            public void onSubscribe(Subscription subscription) {
                System.out.println("onSubscribe");
                this.subscription = subscription;
                this.subscription.request(1);
            }

            // 이 onNext는 다음것이 왔을때 실행하는 메소드이다.
            // 이 메소드에 대해 주의할 점이 있다.
            // 이 메소드는 다음값을 달라고 하는 것이 아니라 다음값이 왔으니 처리하라는 메소드이다.
            // 우리가 Iterator의 next 메소드의 경우는 다음값을 달라고 하는 의미의 메소드이기 파라미터로는 아무것도 받지 않고 return으로 다음값을 return 하게끔 설계되어 있으나
            // 이 Subscriber의 onNext 메소드의 경우는 다음값이 왔으니 처리하라는 의미이기 때문에 파라미터로는 다음값을 받게되고 그 파라미터로 받은 다음값으로 작업을 하는 것이기 때문에 return이 아무것도 없게 되는 구조이다
            // 그래서 이 onNext 메소드의 파라미터는 해당 다음값이 넘어오게 되는 것이다.
            @Override
            public void onNext(Integer i) {
                System.out.println("onNext " + i);
                this.subscription.request(1);
            }

            // 제공자가 구독자에게 보내는 과정에서 예외가 발행했을 경우 이 메소드를 호출한다
            // 제공자가 subscribe 메소드를 통해 구독자를 등록하기 때문에 등록된 구독자의 메소드 호출이 가능하다
            @Override
            public void onError(Throwable throwable) {
                System.out.println("onError");
            }

            // 제공자가 구독자에게 더는 보내줄 값이 없을때 이 메소드를 호출한다
            // 제공자가 subscribe 메소드를 통해 구독자를 등록하기 때문에 등록된 구독자의 메소드 호출이 가능하다
            @Override
            public void onComplete() {
                System.out.println("onComplete");
            }
        };

        publisher.subscribe(subscriber);



    }
}
