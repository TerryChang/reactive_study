package com.terry.reactive.test.day03;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.sql.SQLOutput;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SchedulerTest {

    // Reactive Streams

    /**
     * 지금까지 공부했던 내용을 토대로 단순히 숫자 데이터 5개를 보내서 작업하는 코드
     * 그러나 이 모든 작업이 Main Thread 하나에서 이루어지고 있기 때문에 문제의 소지가 있다
     * 우리가 일상적으로 이러한 작업을 하게 되는 원인은 외부에서 어떤 이벤트가 발생했을 경우 이 이벤트에 대한 내용(또는 작업)등을 구독자(Subscriber)에 전달하는 상황에서 이러한 작업을 하게 되는데
     * 이렇게 Main Thread 하나에서 생산자(Publisher)와 구독자가 모두 실행되는 구조면 구독자는 계속 listeniing 하면서 이벤트를 수신하는 구조를 만들수가 없다
     * Main Thread가 수신대기 하며 계속 Blocking 되고 있는 구조이기 때문에 기타 다른 작업은 아예 멈춰져 버린다
     * 그래서 생산자와 구독자를 동일한 Thread에서 실행되게끔 구조를 잡지 않는다
     */
    @Test
    public void MainThreadPubSub() {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    sub.onNext(1);
                    sub.onNext(2);
                    sub.onNext(3);
                    sub.onNext(4);
                    sub.onNext(5);
                    sub.onComplete();
                }

                @Override
                public void cancel() {

                }
            });
        };

        pub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                logger.info("onSubscribe");
                s.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(Integer integer) {
                logger.info("onNext : {}", integer);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("onError");

            }

            @Override
            public void onComplete() {
                logger.info("onComplete");
            }
        });
    }

    /**
     * 위의 MainThreadPubSub 메소드에서 만든 Publisher와 Subscriber 사이에 중계해주는 Publisher를 하나 더 두어서 이를 구현했다(pub -> subOnPub -> sub)
     * day02에서 Publisher가 제공하는 데이터에서 10을 곱하는 기능을 가지고 있는 중계해주는 Publisher와 동일한 구조이다
     * 그러나 day02 때와는 달리 중계해주는 Publisher인 subOnPub애서 수행하는 기능을 별도의 Thread에서 실행하게끔 하는 구조이다.
     *
     * 여기서는 subOnPub에서 수행하는 기능을 별도 스레드에서 실행하게끔 했다.
     * 이렇게 하는 이유는 Publisher가 Subscriber에 비해 처리속도가 느릴 경우 별도의 스레드에서 Subscriber의 기능이 수행되도록 해서 비동기적인 방식으로 동작되게끔 하여
     * 전체적으로 처리 속도를 개선하기 위해 이런 구조로 만들었다
     * 이 구조가 projectreactor에서 얘기하는 Publisher의 subscribeOn 메소드에서 하는 기능이다.
     */
    @Test
    public void subscribeOn_테스트() {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    sub.onNext(1);
                    sub.onNext(2);
                    sub.onNext(3);
                    sub.onNext(4);
                    sub.onNext(5);
                    sub.onComplete();
                }

                @Override
                public void cancel() {

                }
            });
        };

        Publisher<Integer> subOnPub = sub -> {
            /**
             * Java 5 에서부터 지원이 되는 ExecutorService를 통해 별도 스레드에서 실행하는 것을 구현
             * newSingleThreadExecutor는 1개의 Thread만 제공하는 Thread Pool이다.
             * 이 Thread Pool은 Core Thread 갯수가 1개이고 Maximun Thread 갯수가 1개이기 때문에 1개의 Thread만이 실행되는 것을 보장한다
             * 그렇다고 이걸 사용한다고 해서 새로운 Thread를 요청하지 못하는 그런 것은 아니다.
             * 만약 newSingleThreadExecutor를 이용하는 상황에서 1개의 Thread가 이미 할당받은 상황에서 또 새로운 Thread를 요청하게 될 경우
             * 이 새로운 Thread는 Queue에 담겨져서 대기하고 있다가 기존의 할당받았던 1개의 Thread가 종료되면 이 Queue에 담겨져 있던 Thread를 꺼내서 실행하는 그러한 구조이다
             * 즉 한번에 1개의 Thread만 실행이 되도록 보장이 되어지는 Thread Pool인 것이다
             */

            ExecutorService es = Executors.newSingleThreadExecutor();
            es.execute(() -> pub.subscribe(sub));
            // pub.subscribe(sub);
        };

        subOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                // logger.info("onSubscribe");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onSubscribe");
                s.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(Integer integer) {
                // logger.info("onNext : {}", integer);
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] " + integer);
            }

            @Override
            public void onError(Throwable t) {
                // logger.info("onError");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onError");

            }

            @Override
            public void onComplete() {
                // logger.info("onComplete");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onComplete");
            }
        });

        System.out.println("Exit");
    }

    /**
     * 위의 subscribeOn_테스트가 Publisher가 Subscriber 보다 속도가 느린 경우에 이를 해결하는 거라면
     * publishOn_테스트 메소드는 반대로 Publisher가 Subscriber 보다 속도가 빠른 경우가 이에 속한다
     * 그럴 경우 중간에 만드는 Publisher에서 내부적으로 새로이 Subscriber를 만든뒤 여기에서 Thread로 기존 Subscriber의 메소드를 실행하는 방식을 통해서
     * 속도 개선을 하게 된다
     * 이 구조가 projectreactor에서 얘기하는 Publisher의 publishOn 메소드에서 하는 기능이다.
     *
     * subscribeOn_테스트나 publishOn_테스트를 보면 데이터가 전송이 될때 멀티스레드 형식이기 때문에 데이터가 순서대로 오지 않고 뒤죽박죽 섞이지 않을까 생각할수도 있겠지만..
     * 현재 우리가 사용중인 Executors.newSingleThreadExecutor()는 한번에 한개의 스레드만 실행이 되고 기타 생성된 스레드는 생성된 순서대로 Queue에 대기중이기 때문에
     * 한번에 한 스레드만 순차적으로 실행이 되는 것을 확인할 수 있다
     * 이 구조는 projectreactor에서 구현하고 있는 subscribeOn이나 publishOn 메소드에서도 이러한 구조를 그대로 가지고 있다
     * 즉 멀티스레드 구조를 지닐수는 있겠으나 한번에 한개의 스레드만 실행되도록 하고 있기 때문에 뒤죽박죽 섞이는 구조는 갖고 있지를 않다
     */
    @Test
    public void publishOn_테스트() {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    sub.onNext(1);
                    sub.onNext(2);
                    sub.onNext(3);
                    sub.onNext(4);
                    sub.onNext(5);
                    sub.onComplete();
                }

                @Override
                public void cancel() {

                }
            });
        };

        Publisher<Integer> pubOnPub = sub -> {
            pub.subscribe(new Subscriber<Integer>() {
                ExecutorService es = Executors.newSingleThreadExecutor();
                @Override
                public void onSubscribe(Subscription s) {
                    sub.onSubscribe(s);
                }

                @Override
                public void onNext(Integer integer) {
                    es.execute(() -> sub.onNext(integer));
                }

                @Override
                public void onError(Throwable t) {
                    es.execute(() -> sub.onError(t));
                }

                @Override
                public void onComplete() {
                    es.execute(() -> sub.onComplete());
                }
            });
        };

        pubOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                // logger.info("onSubscribe");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onSubscribe");
                s.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(Integer integer) {
                // logger.info("onNext : {}", integer);
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] " + integer);
            }

            @Override
            public void onError(Throwable t) {
                // logger.info("onError");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onError");

            }

            @Override
            public void onComplete() {
                // logger.info("onComplete");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onComplete");
            }
        });

        System.out.println("Exit");
    }

    /**
     * subscribeOn과 publishOn을 동시에 구현한 테스트
     */
    @Test
    public void subscribeOn_publishOn_테스트() {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    sub.onNext(1);
                    sub.onNext(2);
                    sub.onNext(3);
                    sub.onNext(4);
                    sub.onNext(5);
                    sub.onComplete();
                }

                @Override
                public void cancel() {

                }
            });
        };

        Publisher<Integer> subOnPub = sub -> {
            ExecutorService es = Executors.newSingleThreadExecutor();
            es.execute(() -> pub.subscribe(sub));
            // pub.subscribe(sub);
        };

        Publisher<Integer> pubOnPub = sub -> {
            subOnPub.subscribe(new Subscriber<Integer>() {
                ExecutorService es = Executors.newSingleThreadExecutor();
                @Override
                public void onSubscribe(Subscription s) {
                    sub.onSubscribe(s);
                }

                @Override
                public void onNext(Integer integer) {
                    es.execute(() -> sub.onNext(integer));
                }

                @Override
                public void onError(Throwable t) {
                    es.execute(() -> sub.onError(t));
                }

                @Override
                public void onComplete() {
                    es.execute(() -> sub.onComplete());
                }
            });
        };

        pubOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                // logger.info("onSubscribe");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onSubscribe");
                s.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(Integer integer) {
                // logger.info("onNext : {}", integer);
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] " + integer);
            }

            @Override
            public void onError(Throwable t) {
                // logger.info("onError");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onError");

            }

            @Override
            public void onComplete() {
                // logger.info("onComplete");
                String threadName = Thread.currentThread().getName();
                System.out.println("[" + threadName + "] onComplete");
            }
        });

        System.out.println("Exit");
    }
}
