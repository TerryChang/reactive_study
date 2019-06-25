package com.terry.reactive.test.day04;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
public class FutureTest {

    /**
     * CachedThreadPool을 통해서 생성된 Thread를 통해 Thread를 실행시키는데 해당 Thread는 return 값이 없어서 그냥 실행시키는 위주로만 가는 경우엔
     * ExecutorService 의 execute 메소드에 해당 return 값 없는 Runnable 인터페이스 구현 객체를 인자로 주어 실행하게 해주면 된다
     */
    @Test
    public void return값_없는_Thread_실행() throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {

            }
            logger.info("Hello");
        });
        logger.info("Exit");
        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * 위의 Test와는 달리 return 값이 있는 Thread를 실행시키고자 할 경우엔 Callable 인터페이스를 구현한 객체를 ExecutorService의 submit 메소드 인자값으로 넘겨주면 된다
     * Callable 인터페이스의 경우는 예외를 던지는 것도 가능하기 때문에 Runnable의 경우 try-catch문으로 감싸지 않고 밖으로 예외를 던져서 처리시키게도 가능하다
     * 이렇게 Callable 인터페이스를 구현한 객체가 Thread로 동작하여 return 한 데이터를 받고자 할때 Future 인터페이스를 구현한 클래스를 통해 결과를 받을수 있다
     * return 한 결과를 받을때는 Future 인터페이스의 get 메소드를 통해 받을 수 있다.
     * 이때 주의할 점이 있다. get 메소드를 호출하여 결과를 받을려고 하게 되면 get 메소드를 실행하는 시점부터 blocking이 걸리기 때문에
     * 결과값이 올때까지는 get 메소드를 호출한 이후의 코드가 실행이 되지를 않는다
     * 그래서 비동기 효과를 볼려면 그 return 값을 실제로 사용하는 시점까지 호출을 최대한 늦춰야 비동기 효과를 볼 수 있다
     * (미리 받아놓겠다는 식으로 변수에 할당하는 작업을 먼저 진행하면 그 지점에서 blocking이 걸려서 대기상태에 빠지기 때문에 별도 Thread로 만들어서 실행하는 의미가 없어진다)
     *
     * Future 클래스의 isDone 메소드를 사용해서 해당 Thread의 작업종료 여부를 알 수 있다. Thread 작업이 종료가 안되었을 경우 Main Thread에서 특정 작업을 해야 한다거나 할때 그럴때 유용하다
     *
     * Future 클래스는 비동기 작업의 결과를 받아올 수 있는 방법(Handler)이지 비동기 작업의 결과 그 자체는 아니다
     * Java에서 비동기 작업의 결과를 가져오는 방법으로는 Future와 Callback을 이용하는 2가지 방법이 있다
     */
    @Test
    public void Future를_이용한_return값_있는_Thread_실행() throws ExecutionException, InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        Future<String> f = es.submit(() -> {
            Thread.sleep(2000);
            logger.info("Async");
            return "hello";
        });
        // System.out.println(f.get()); // 여기서 get 메소드를 호출하여 결과를 받을려고 하면 결과가 올때까지 blocking이 되기 때문에 결과가 올때까지는 그 다음 코드가 실행이 이루어지지 않는다
        logger.info(String.valueOf(f.isDone()));
        Thread.sleep(4000);      // 이 지점에서 2초 delay를 준 그동안 es.submit으로 실행되는 Thread가 작업을 하면서 완료 상태가 되는 것이다
        logger.info("exit");
        logger.info(String.valueOf(f.isDone())); // 이때 체크해보면 es.submit으로 실행되는 Thread가 작업완료가 된다
        logger.info(f.get());
        es.shutdown();
        es.awaitTermination(30, TimeUnit.SECONDS);
    }

    /**
     * 위의 예제를 FutureTask 클래스를 이용해서 구현했다
     * Future를 이용한 작업은 ThreadPool에 어떤 작업을 던지고 그 결과를 받는 컨셉으로 되어 있지만
     * FutureTask 클래스는 Thread로 해야하는 작업까지도 같이 객체 형태로 정의를 할 수 있다.
     *
     * 이때 FutureTask 를 익명 클래스 객체로 정의하면서 done 매소드를 override해서 재정의를 할 수 있다.
     * done 메소드는 FutureTask 클래스 객체에 정의되어 있는 작업이 완료된 뒤 어떤 작업을 해야 할지에 대한 정의를 할 수 있는데
     * 이 메소드에서 결과값을 이용해서 어떤 작업을 수행하게끔 만든다면 그것이 callback 메소드 개념이 되는 것이다
     * (그러나 우리가 흔히 아는 callback의 경우는 메소드를 외부에서 제작해서 메소드 자체를 인자값으로 던지는 방법을 많이 쓰다보니
     * 이렇게 객체를 만들때 해당 callback 메소드를 정의하는식의 방법은 재활용도 안되어서 실용적인 가치가 없어보인다. 우리가 흔히 쓰는 callback 메소드와는 좀 동떨어져 있는 구현?)
     */
    @Test
    public void FutureTask를_이용한_return값_있는_Thread_실행() throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        FutureTask<String> f = new FutureTask<String>(() -> {
            Thread.sleep(2000);
            logger.info("Async");
            return "hello";
        }) {
            @Override
            protected void done() {
                try {
                    System.out.println(get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        es.execute(f); // FutureTask 클래스를 이용할 경우엔 ExecutorService의 execute 메소드를 이용해서 실행시킨다(Future를 통해 결과를 받는 그런 방법이 아니기 때문에 execute 메소드에 바로 해당 객체를 던져서 실행시켜버린다)
        logger.info(String.valueOf(f.isDone()));
        Thread.sleep(4000);
        logger.info("exit");
        logger.info(String.valueOf(f.isDone()));
        es.shutdown();
        es.awaitTermination(30, TimeUnit.SECONDS);
    }

    interface SuccessCallback {
        void onSuccess(String result);
    }

    interface ExceptionCallback {
        void onError(Throwable t);
    }

    class FutureTaskCallback extends FutureTask<String> {
        SuccessCallback sc;
        ExceptionCallback ec;

        public FutureTaskCallback(Callable<String> callable, SuccessCallback sc, ExceptionCallback ec) {
            super(callable);
            this.sc = Objects.requireNonNull(sc); // requireNonNull 메소드는 파라미터인 sc가 null로 들어올 경우 NullPointerException이 던져진다
            this.ec = Objects.requireNonNull(ec);
        }

        @Override
        protected void done() {
            try {
                sc.onSuccess(get());
            } catch (InterruptedException e) {
                // InterruptedException 예외는 현재 작업을 중지하고 종료하라고 설정하는 성격의 예외이기 때문에
                // 이 예외를 받았을때 먼가 다르게 처리한다는 것은 이 예외의 성격상 맞지가 않다.
                // 그래서 이 예외를 받았을때는 현재 Thread를 종료하라는 신호를 주는식으로 코딩해도 충분하다
                // 종료전에 무언가 작업을 해야 하는 것이 있을수는 있겠으나(예를 들어 로깅 등)
                // 최종 작업은 Thread  종료 신호를 보내주는 것으로 설정하는 것이 바람직하다
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // ExecutionException은 예외가 발생했을 경우 해당 예외를 한번 포장해서 던져지는 예외이다.
                // 예를 들어 NullPointerException이 발행했다 하더라도 그걸 ExecutionException 으로 한번 감싸서 던지는 식인 것이다
                // 그래서 ExecutionException을 바로 onError 메소드 파라미터에 주지 말고 getCause 메소드를 통해 원래의 예외를 가져와서 그걸 파라미터로 넘겨주는게 좋다
                ec.onError(e.getCause());
            }
        }


    }

    /**
     * FutureTask를_이용한_return값_있는_Thread_실행 메소드에서 보인 callback은 FutureTask 객체를 만드는 시점에 callback 메소드를 정의하는 것이기 때문에
     * 우리가 흔히 아는 callback 메소드를 주입하는 방식과는 동떨어져있다.
     * 그래서 SuccessCallback 인터페이스에서 callback 메소드의 인터페이스를 정의한 후 이 인터페이스를 FutureTask 클래스를 상속받은 클래스에 구현시켜서..
     * 해당 클래스의 생성자에 callback 메소드를 주입하는 형식으로 이를 구현했다
     */
    @Test
    public void Callback_메소드를_외부에서_주입할수있는_FutureTask를_이용한_return값_있는_Thread_실행() throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        FutureTaskCallback f = new FutureTaskCallback(() -> {
            Thread.sleep(2000);
            // if(1 == 1) throw new RuntimeException("Async Error"); // 예외 처리에 대한 테스트를 진행하고자 할 때는 이 라인의 주석을 푼다
            logger.info("Async");
            return "hello";

        }, (result) -> {
            logger.info(result);
        }, (exception) -> {
            logger.error(exception.getMessage());
        });

        es.execute(f); // FutureTask 클래스를 이용할 경우엔 ExecutorService의 execute 메소드를 이용해서 실행시킨다(Future를 통해 결과를 받는 그런 방법이 아니기 때문에 execute 메소드에 바로 해당 객체를 던져서 실행시켜버린다)
        logger.info(String.valueOf(f.isDone()));
        Thread.sleep(4000);
        logger.info("exit");
        logger.info(String.valueOf(f.isDone()));
        es.shutdown();
        es.awaitTermination(30, TimeUnit.SECONDS);
    }
}
