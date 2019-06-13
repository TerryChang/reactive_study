package com.terry.reactive.test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Pubsub {

    // Publisher <-- Observable
    // Subscriber <-- Observer
    // Subscriber는 다음의 메소드를 호출한다
    // onSubscribe -> onNext* -> (onError | onComplete)
    // onSubscribe 메소드를 먼저 호출한뒤 onNext 메소드를 0번 이상 호출하고 마지막으로 onComplete 메소드나 onError 메소드를 호출해야 한다

    @Test
    public void onPublisherTest() throws InterruptedException {
        Iterable<Integer> iter = Arrays.asList(1,2,3,4,5);
        ExecutorService es = Executors.newCachedThreadPool();
        
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
                  
                    // 하나의 구독자에게 데이터를 전송할때 작업 시간이 오래 걸려서 이를 제공자 측에서 Thread로 병렬처리 할려고 할 수 있겠으나..
                    // 스펙에서는 불가능하다고 정의되어 있다(스펙상에서는 제공자는 구독자에게 순차적으로 데이터를 전송하는 것으로 정의, 한 스레드에서만 데이터가 구독자에게 보내어지는 것이다)
                    // RxJava가 초창기 버전에서 병렬처리를 구현했으나 구현하고서 보니 오히려 더 지저분해지고..
                    // 이런 구현으로 인해 발생한 단점이 장점을 상쇄할 정도여서 이렇게 제공자에서 멀티스레드로 하나의 구독자에게 병렬로 제공하는.. 
                    // 그런 구현이 현재는 빠진 상태라고 한다
                    @Override
                    public void request(long n) {
                      // 별도의 단일 스레드에서 데이터를 전송한다(비동기 방식)
                      // 만약 결과값을 받아서 별도로 처리하고 싶을 경우 
                      // Future<?> f = es.submit(() -> {
                      // 요렇게 해서 Future 객체로 받는다
                      // Future로 결과값을 받게 할 경우 작업 중간에 cancel을 걸 수 있다
                      es.execute(() -> {
                        int i = 0;
                        try {
                          // 람다식 밖에서 선언된 변수 n을 람다식 안에서 가공할 수 없다. n-- 요런식으로 작업을 할 수 없다
                          // 그래서 람다식 안에서 별도로 변수를 선언한 다음에 이 변수 값을 가공하여 밖에서 정의된 변수 n과 비교하는 방법을 사용한다
                          while(i++ < n) {                            // n번 만큼 반복
                              if (iterator.hasNext()) {
                                  System.out.println(Thread.currentThread().getName() + " call onNext Method");
                                  subscriber.onNext(iterator.next()); // 구독자에게 데이터를 전송한다
                              } else {
                                  System.out.println(Thread.currentThread().getName() + " call onComplete Method");
                                  subscriber.onComplete();
                                  break;
                              }
                          }
                        } catch(Exception e) {
                          subscriber.onError(e);
                        }
                      });
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
                System.out.println(Thread.currentThread().getName() + " onSubscribe");
                this.subscription = subscription;
                this.subscription.request(1);
            }

            // 이 onNext는 다음것이 왔을때 실행하는 메소드이다.
            // 이 메소드에 대해 주의할 점이 있다.
            // 이 메소드는 다음값을 달라고 하는 것이 아니라 다음값이 왔으니 처리하라는 메소드이다.
            // 우리가 Iterator의 next 메소드의 경우는 다음값을 달라고 하는 의미의 메소드이기 파라미터로는 아무것도 받지 않고 return으로 다음값을 return 하게끔 설계되어 있으나
            // 이 Subscriber의 onNext 메소드의 경우는 다음값이 왔으니 처리하라는 의미이기 때문에 파라미터로는 다음값을 받게되고 그 파라미터로 받은 다음값으로 작업을 하는 것이기 때문에 return이 아무것도 없게 되는 구조이다
            // 그래서 이 onNext 메소드의 파라미터는 해당 다음값이 넘어오게 되는 것이다.
            // 다음 데이터를 가져오라는 명령도 여기에서 Subscription 객체의 request 메소드를 호출한다
            @Override
            public void onNext(Integer i) {
                System.out.println(Thread.currentThread().getName() + " onNext " + i);
                this.subscription.request(1);       // 다음 것을 달라고 요청한다
            }

            // 제공자가 구독자에게 보내는 과정에서 예외가 발행했을 경우 이 메소드를 호출한다
            // 제공자가 subscribe 메소드를 통해 구독자를 등록하기 때문에 등록된 구독자의 메소드 호출이 가능하다
            @Override
            public void onError(Throwable throwable) {
                System.out.println("onError : " + throwable.getMessage());
            }

            // 제공자가 구독자에게 더는 보내줄 값이 없을때 이 메소드를 호출한다
            // 제공자가 subscribe 메소드를 통해 구독자를 등록하기 때문에 등록된 구독자의 메소드 호출이 가능하다
            @Override
            public void onComplete() {
                System.out.println(Thread.currentThread().getName() + " onComplete");
            }
        };

        publisher.subscribe(subscriber);

        // awaitTermination 메소드를 실행시켜 ExecutorService 객체에 delay를 걸어주도록 해야 한다
        // 왜냐면 위의 request 메소드에서 ExecutorService 객체의 execute 메소드를 통해 별도 스레드로 구독자에게 데이터를 전달하고 있는데..
        // 전달 작업이 다 끝나기 전에 ExecutorService 객체의 shutdown 메소드를 실행시켜 ExecutorService 객체에서 할당된 스레드를 작업종료 시켜버리면 예외가 발생하기 때문이다.
        // 그래서 request 메소드에서 별도 스레드로 실행중인 구독자 데이터 전달 작업이 끝날때까지 인위적으로 ExecutorService 메소드의 awaitTermination 메소드를 통해서
        // 인위적으로 대기를 걸어주고 shutdown 메소드를 통해 ExecutorService 객체를 shutdown 한다
        // 동영상에서는 그런 용도로 사용하고 있는것 같은데 지금 현재 이와 관련되어 사용중인 클래스 및 메소드에 대한 설명과 매치시켜보면 좀 맞지 않는 구석이 있다.
        // https://palpit.tistory.com/732 여기에 관련 클래스 및 메소드에 대한 설명이 있다
        es.awaitTermination(10, TimeUnit.HOURS);
        es.shutdown();

    }
}
