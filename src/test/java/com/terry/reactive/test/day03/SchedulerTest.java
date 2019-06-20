package com.terry.reactive.test.day03;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.sql.SQLOutput;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
     *
     * 동영상에서 나온대로 코딩할 경우 로그가 출력이 되지 않는 문제가 있었다
     * 그래서 okky에서 질문도 올려봤었는데..내가 만든 코드로 똑같이 만들었는데 잘 이루어졌다
     * 그래서 고민고민해보니..첫번째 강의 시간때 ExecutorService 클래스 객체를 통해 할당되어진 스레드 작업들이 종료가 되기도 전에
     * ExecutorService 객체가 종료되어서 출력이 안되었던 기억이 생각났다.
     * 그래서 다음과 같이
     *
     * es.awaitTermination(10, TimeUnit.SECONDS);
     *
     * 이 코드를 넣어서 이 부분을 해결했다. 그리고 이걸 넣으면 InterruptedException이 발생하게 되는데
     * 이 예외처리를 Subscriber의 onError 메소드로 던지게끔 했다(이런 구조가 맞는것이고..)
     */
    @Test
    public void subscribeOn_테스트() {
        logger.info("Start Test");
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
            try {
                es.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                sub.onError(e);
            }
        };

        subOnPub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                logger.info("onSubscribe");
                String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onSubscribe");
                s.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(Integer integer) {
                logger.info("onNext : {}", integer);
                String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] " + integer);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("onError");
                String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onError");

            }

            @Override
            public void onComplete() {
                logger.info("onComplete");
                String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onComplete");
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
     *
     * 여기에서도 subscribeOn_테스트 메소드때와 마찬가지로 로그 출력과 관련하여 해결했던 방법을 그대로 적용했다
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
                    try {
                        es.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        onError(e);
                    }
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
                logger.info("onSubscribe");
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onSubscribe");
                s.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(Integer integer) {
                logger.info("onNext : {}", integer);
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] " + integer);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("onError");
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onError");

            }

            @Override
            public void onComplete() {
                logger.info("onComplete");
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onComplete");
            }
        });

        System.out.println("Exit");
    }

    /**
     * subscribeOn과 publishOn을 동시에 구현한 테스트
     *
     * 여기에서도 subscribeOn_테스트 메소드때와 마찬가지로 로그 출력과 관련하여 해결했던 방법을 그대로 적용했다
     */
    @Test
    public void subscribeOn_publishOn_테스트() {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    logger.info("request");
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
             * Executors.newSingleThreadExecutor() 이렇게 사용할 경우 생성되는 thread 이름이 pool-1-thread-1 이런식으로 Thread pool 이름인 pool-1-thread- 이런식으로 주어져서 의미 파악을 하기 어렵다
             * 그래서 Thread 이름의 Prefix를 설정할 수 있게끔 Spring(정확하게는 Spring-Context)에서 제공하는 CustomizableThreadFactory의 getThreadNamePrefix 메소드를 이용해서
             * 인위적으로 Thread 이름의 Prefix를 지정할 수 있다
             * 이게 가능한 이유는 newSingleThreadExecutor 메소드에 들어가는 객체가 ThreadFactory Interface를 구현한 객체여야 하는데 CustomizableThreadFactory 클래스가 이 인터페이스를 구현했기 때문이다
             */
            ExecutorService es = Executors.newSingleThreadExecutor(new CustomizableThreadFactory(){
                @Override
                public String getThreadNamePrefix() {
                    return "sub-";
                }
            });
            es.execute(() -> pub.subscribe(sub));
            try {
                es.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                sub.onError(e);
            }
            // pub.subscribe(sub);
        };

        Publisher<Integer> pubOnPub = sub -> {
            subOnPub.subscribe(new Subscriber<Integer>() {
                /**
                 * 여기에서도 Thread 이름을 알기 쉽게끔 위에서 했던 작업과 마찬가지로 CustomizableThreadFactory 클래스 객체를 newSingleThreadExecutor 메소드에 사용했다
                 */
                ExecutorService es = Executors.newSingleThreadExecutor(new CustomizableThreadFactory(){
                    @Override
                    public String getThreadNamePrefix() {
                        return "pub-";
                    }
                });
                @Override
                public void onSubscribe(Subscription s) {
                    sub.onSubscribe(s);
                    try {
                        es.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        onError(e);
                    }
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
                logger.info("onSubscribe");
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onSubscribe");
                s.request(Long.MAX_VALUE);

            }

            @Override
            public void onNext(Integer integer) {
                logger.info("onNext : {}", integer);
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] " + integer);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("onError");
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onError");

            }

            @Override
            public void onComplete() {
                logger.info("onComplete");
                // String threadName = Thread.currentThread().getName();
                // System.out.println("[" + threadName + "] onComplete");
            }
        });

        System.out.println("Exit");
    }

    /**
     * interval 메소드는 주기를 주어서 해당 주기 간격으로 0부터 1씩 증가한 값을 return 해주는 메소드이다.
     * 해당 작업의 종료는 Long.MAX_VALUE 까지로 보인다(API 문서를 보면 이 메소드가 return 하는 타입이 Flux<Long> 이어서..)
     * 이 과정에서 별도 Thread를 생성해서 0부터 1씩 증가한 값을 return 하게 되는데
     * 실제 테스트 해보면 아무것도 출력하지 않는다
     * 그리고 소스 마지막 부분에 코딩한 logger.info("exit")만 나오게 된다
     * 주석처리한 TimeUnit.SECONDS.sleep(5); 이 실행되게끔 주석을 풀어주고 실행되면 5초동안 0.5초 간격으로 0부터 1씩 증가한 값을 주지만 5초가 지나면 종료된다
     *
     * 여기서 알아야 할 지식이 있는데
     * Java 에서 Thread는 User Thread와 Daemon Thread 이렇게 2 종류로 나눌수 있다.
     * User Thread는 사용자가 직접 생성한 Thread로서 User Thread를 생성한 Main Thread의 작업이 종료되어도 User Thread는 종료하지 않는 한에는 자바 프로그램이 종료되지 않는다
     * 그러나 Daemon Thread인 경우 Main Thread의 작업이 종료되어도 현재 실행중인 Thread가 모두 Daemon Thread만 있다면 Daemon Thread의 종료 여부와는 상관없이 자바 프로그램이 종료된다
     *
     * interval의 경우 전송되는 데이터 갯수에 제한이 걸리지 않기 때문에 이를 제한을 걸려면 take 메소드를 실행한다
     * 주석처리된 take(3)은 interval 메소드를 통해 0부터 1씩 증가된 값을 받아오게 되는데 이를 3개만 받겠다는 뜻이다.
     */
    @Test
    public void FluxInterval() throws InterruptedException {
        Flux.interval(Duration.ofMillis(500))
                // .take(3)
                .subscribe(s -> logger.info("onNext : {}", s));
        logger.info("exit");

        TimeUnit.SECONDS.sleep(5);
    }

    /**
     * 위의 FluxInterval 메소드같이 Pivotal의 Reactor 라이브러리를 이용한게 아니라
     * 직접 reactive-streams 에서 제공하는 표준 인터페이스로 직접 구현한 것이다.
     * 그러나 이 코드를 실행해보면 제대로 실행이 되질 않는다.
     * 이것을 Junit을 이용한 Test 메소드가 아닌 일반 Java 클래스의 main 메소드에 실행시키면 정상 동작한다
     * 추측엔 Test 클래스 안에서 scheduleAtFixedRate 메소드를 통해 생성된 Thread는 Daemon Thread이고
     * 일반 클래스의 main 메소드 안에서 scheduleAtFixedRate 메소드를 통해 생성된 Thread는 User Thread이지 않을까 하는 생각을 해본다
     * 이러한 상황은 비단 이 메소드 뿐만 아니라 현재까지 진행됐던 코딩 중 awaitTermination 메소드를 사용해야만 정상적으로 동작되는 메소드들 또한 그런것이 아닐까 하고 추측해본다
     * @throws InterruptedException
     */
    @Test
    public void FluxIntervalUsingNative() throws InterruptedException {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                int no = 0;
                boolean canceled = false;
                @Override
                public void request(long n) {
                    // 주어진 간격으로 Thread가 실행되는 newSingleThreadScheduledExecutor 메소드를 사용한다
                    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                    // Thread를 일정 시간 간격으로 실행하는 메소드인 scheduleAtFixedRate 메소드를 사용한다
                    // 첫번째 파라미터는 실행하고자 하는 Thread로 여기서는 Lambda 식으로 구현했고
                    // 두번째 파라미터는 처음 실행시 지연해야 할 시간을 정하고
                    // 세번째 파라미터는 두번째 실행부터 고정적으로 주어지는 실행간격 시간을 정하고
                    // 네번째 파라미터는 두번째와 세번째 파라미터에서 설정한 시간의 단위를 지정하는 것이다
                    // 두번째와 세번째 파라미터의 의미가 잘 와닿지 않을 수 있는데 구체적인 예를 들자면
                    // 처음 실행할때 바로 실행되는 것이 아니라 300 밀리세컨드 지연시킨뒤 실행시켰다가
                    // 두번째 부터는 500 밀리세컨드 간격으로 반복적으로 실행되는 그런 케이스를 정할때 사용하는 것이다
                    // 지금 설정한 값은 두번째 파라미터는 0을 주었기 때문에 처음 실행시 지연없이 바로 Thread를 실행하고
                    // 처음 실행을 시작한 뒤 300 밀리세컨드 간격으로 Thread가 실행된다
                    // 여기서 주의할 점이 있는데 세번째 파라미터에 주어지는 간격은 Thread가 시작한 시간을 기준으로 한 간격이다
                    // 예를 들어 실행을 완료하는데 800 밀리 세컨드가 소요되는 Thread가 있다고 가정하자
                    // scheduleAtFixedRate 메소드를 이용해서 실행할 경우 세번째 파라미터로 300을 주었으면 Thread가 종료되기도 전에 다음번 Thread가 실행되는 것이다
                    // 만약 해당 간격을 실행되는 Thread가 종료된 뒤의 간격으로 잡고자 할 경우엔 scheduleWithFixedDelay 메소드를 사용하면 된다
                    // scheduleWithFixedDelay 메소드의 파라미터들도 scheduleAtFixedRate와 같다
                    exec.scheduleAtFixedRate(() -> {
                        if(canceled) {
                            exec.shutdown();
                            return;
                        }
                        sub.onNext(no++);
                    }, 0, 300, TimeUnit.MILLISECONDS);
                }

                @Override
                public void cancel() {
                    canceled = true;
                }
            });
        };

        Publisher<Integer> takepub = sub -> {
            pub.subscribe(new Subscriber<Integer>() {
                int counter = 1;
                Subscription subc;
                @Override
                public void onSubscribe(Subscription s) {
                    this.subc = s;
                    sub.onSubscribe(s);
                }

                @Override
                public void onNext(Integer integer) {
                    if(counter++ > 10) {
                        subc.cancel();
                    } else {
                        sub.onNext(integer);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    sub.onError(t);
                }

                @Override
                public void onComplete() {
                    sub.onComplete();
                }
            });
        };

        takepub.subscribe(new Subscriber<Integer>() {
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
                logger.info("onError : {}", t);
            }

            @Override
            public void onComplete() {
                logger.info("onComplete");
            }
        });

        // 테스트 메소드에서 동작을 확인할려면 sleep 메소드를 걸어주면 해당 주기만큼은 실행되는 것을 볼 수 있다
        // Thread.sleep(10000);
    }
}
