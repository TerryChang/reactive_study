package com.terry.reactive.day04;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@SpringBootApplication
@Slf4j
@EnableAsync
public class Day04SpringBootApplication {

    /**
     * Bean의 메소드를 비동기적으로 실행시킬려면 @Async 어노테이션을 붙이면 해당 Bean의 메소드가 별도 Thread에서 실행이 이루어진다
     * @Async 어노테이션이 동작되도록 할려면 @EnableAsync 어노테이션을 @Confifuration 어노테이션이 붙은 클래스에 붙여줘야한다
     * @SpringBootApplication 어노테이션은 내부적으로 @Configuration 어노테이션이 붙은 클래스 역할도 같이 하게끔 해주기 때문에 이 Dat04SpringBootApplication 클래스에 @EnableAsync 어노테이션을 붙여서 비동기 동작을 할 수 있게 설정할 수 있다
     *
     * 비동기적인 메소드 실행에 대한 결과를 받을때 Future 인터페이스 구현 객체로 return 받아서 get 메소드로 결과를 가져오거나 isDone 메소드로 동작의 완료 여부를 확인할 수 있다
     *
     * @Async 어노테이션을 별다른 설정 없이 그냥 사용하면 이 어노테이션은 SimpleAsyncTaskExecutor 클래스 Bean을 통해 Thread를 만들게 되는데
     * 이 클래스는 Thread 요청이 100개가 오게 되면 Thread를 100개를 만들게 된다(같은 코드의 Thread라 해도 재사용하지 않고 매번 Thread를 재생성해서 작업하게 된다(캐시 등의 효율을 올리기 위한 어떠한 기능도 없다)
     * 그래서 테스트 용도가 아닌 한에는 실무적으로 사용할때는 좋지 않는 클래스이다
     * 그러나 java.util.concurrent.Executor 나 org.springframework.core.task.TaskExecutor 타입 Bean이 등록되어 있으면 @Async 어노테이션은 해당 타입의 Bean을 이용해 Thread를 할당받아 구현한다
     * 또 java.util.concurrent.Executor 나 org.springframework.core.task.TaskExecutor 타입 Bean이 여러개 등록되어 있는 상황일 경우 @Async(bean 이름)을 주면 지정된 이름의 bean에서 제공되는 Thread를 할당받아 구현하게 된다
     *
     *
     */
    @Component
    @Async
    public static class MyService {
        public Future<String> hello() throws InterruptedException {
            logger.info("hello()");
            Thread.sleep(2000);
            return new AsyncResult<>("Hello");
        }

        /**
         * ListenableFuture 클래스 객체로 return 하게 해주면 이 메소드를 호출한 곳에서 이 메소드 작업에 대한 callback 메소드(success, error)를 등록할 수 있다.
         * ListenableFuture 클래스는 Spring에서 제공하는 클래스이다
         */
        public ListenableFuture<String> listenableFuture() throws InterruptedException {
            logger.info("listenableFuture.hello()");
            Thread.sleep(2000);
            return new AsyncResult<>("listenableFuture.Hello");
        }
    }

    /**
     * Thread Pool은 처음에는 Thread를 전혀 만들지 않고 있다가 처음으로 Thread 요청이 들어오면 그때 설정된 Core Pool Size 갯수 만큼 Thread를 만든다
     *
     * Thread Pool은 DB Connection Pool과는 동작 방식에 있어서 차이점이 있다.
     * Db Connection Pool init pool size 만큼 만들었다가 그걸로도 부족하면 max pool size 만큼 만들고 max pool size 만큼으로도 부족하면 대기열 queue size만큼 대기열 queue에 요청을 넣어두는 방식이라면
     * Thread Pool은 core pool size 만큼 만들었다가 그걸로도 부족하면 대기열 queue size 만큼 대기열 queue에 요청을 넣어두고 대기열 queue도 꽉 차서 더는 요청을 받을수 없게 되면 그때 max pool size만큼 Thread를 추가로 만들어주게 된다
     * DB Connection Pool이 init -> max -> queue로 전개되는것과는 달리
     * Thread Pool은 core -> queue -> max 로 전개되는 것이다
     *
     * 동영상을 보고 좀더 이 부분에 대해 조사를 해봤는데 동영상 설명이 오해를 살 소지가 있어서 좀더 보강 설명을 해둔다
     * 비교작업을 하면서 한꺼번에 core pool size 만큼, max pool size 만큼 만드는 것이 아니라 한번 요청할때 여유분이 없으면 새로 1개의 Thread를 만들어서 Pool에 보관하는 방식이다.
     *
     * 1. 처음에 아무것도 없다가 Thread 요청이 들어오면 Thread를 1개 만들어서 Pool에 넣고 이 Thread를 return 한다
     * 2. 요청이 들어오면 또 Thread를 1개 만들어서 Pool에 넣고 이 Thread를 return 하고..
     * 3. 이렇게 작업하다가 Core Pool Size 만큼 Thread Pool이 차 버린 상태에서 다시 Thread 요청이 들어오면 대기열 Queue에 요청을 넣어두게 된다.
     * 4. 그러다가 대기열 Queue도 꽉 차게 되면 현재 Thread pool에 들어있는 Thread 갯수( = core pool size)가 max pool size 보다 작으면 새로 Thread를 1개 만든뒤 Pool에 넣는다
     * 5. 이러한 상황에서 대기열 Queue도 꽉 차고, Thread Pool에 들어있는 Thread 갯수도 max pool size 만큼 되면 이 이후의 Thread 요청은 거절된다
     *
     * 이것은 어디까지나 Thread를 재활용하지 않는 Thread Pool의 구조이다. 즉 Thread는 사용이 끝나면 해당 Thread 가 없어지기 때문에 다시 요청이 들어오면 다시 재생성을 하는 개념이다.
     * 여기에다가 Cache 등을 적절히 섞어서 재활용을 시켜주는 것이다
     *
     * 관련 내용에 대한 자료는 http://www.bigsoft.co.uk/blog/2009/11/27/rules-of-a-threadpoolexecutor-pool-size 를 참조했다
     *
     */
    @Bean
    ThreadPoolTaskExecutor tp() {
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(10); // Thread Pool의 Core Pool Size를 정한다
        te.setMaxPoolSize(100); // Thread Pool의 Max Pool Size를 정한다
        te.setQueueCapacity(200); // Thread Pool의 대기열 Queue Size를 정한다
        // te.setKeepAliveSeconds(10); // Thread Pool이 core pool size보다 큰 크기로 Thread가 차 있는 상태에서 Thread Pool에 Thread가 반납이 되었는데 이 Thread가 setKeepAliveSeconds 메소드에 지정된 시간만큼 사용중이지 않은 상태를 유지하면 해당 Thread를 제거한다
        // te.setTaskDecorator(null); // Thread를 실행하는 지점 의 이전과 이후에 별도의 코드를 넣어서 실행이 되게끔 한다(Spring의 AOP를 생각하면 된다)
        te.setThreadNamePrefix("myThread"); // Thread 이름 앞에 특정 문자열을 prefix로 설정한다
        return te;
    }


    public static void main(String [] args) {
        // 이렇게 코딩하면 Spring Boot가 띄우는 Embedded WAS가 백그라운드로 계속 실행되는 그런 상태가 아니라 바로 종료가 되게끔 할 수 있다
        try(ConfigurableApplicationContext c = SpringApplication.run(Day04SpringBootApplication.class)){

        };

        // SpringApplication.run(Day04SpringBootApplication.class);
    }

    @Autowired
    MyService myService;
    /**
     * Spring Boot가 뜨면서 바로 실행이 되는 어떤 코드를 만들때 ApplicationRunner 인터페이스 구현 객체를 Bean으로 생성해주면 된다
     * 모든 Bean들이 다 설정들이 이루어진 뒤에야 이 ApplicationRunner 타입 객체가 실행이 된다
     *
     * 지금 이 ApplicationRunner Bean을 Controller 라고 생각해보면 우리가 myService.hello() 같이 작업이 오래 걸리는 메소드를 별도 Thread로 실행되도록 한 뒤에 이 결과를 받아 처리할려면 어떻게 할까?
     * (작업이 오래 걸리는 메소드를 작업이 끝날때까지 기다렸다가 결과를 보여주는 것이 아니라 별도 Thread에서 작업이 이루어지게 하였기 때문에 그 결과는 Controller의 다른 메소드에서 알아낼 수 밖에 없다)
     * 1. Database등의 별도 보관장치에다가 결과를 기록해둔뒤에 다른 Controller에서 이것을 읽어오는 방법을 사용
     * 2. Future 클래스 객체를 HttpSession에 넣어둔뒤 다른 Controller에서 HttpSession에 저장되어 있는 Future 클래스 객체를 접근하여 읽어오는 방법
     * (동영상 강의에서는 Future 클래스를 저장하는 것으로 언급했지만 방법만 놓고 보면 Future 클래스 객체가 아니더라도 작업 과정과 결과를 저장할 수 있는 클래스라면 해당 클래스를 HttpSession에 저장해도 될것 같다)
     */
    @Bean
    ApplicationRunner run() {
        return args -> {
            logger.info("run()");
            Future<String> f = myService.hello();
            logger.info("exit : {}", f.isDone());
            logger.info("result : {}", f.get());
        };
    }

    /**
     * ListenableFuture 클래스로 비동기 작업 결과를 return 받으면 해당 작업에 대한 success callback과 exception callback 메소드를 설정할 수 있다.
     * 이렇게 callback 메소드를 등록하면 listenableFutureRun 메소드의 작업이 종료되더라도 ListenableFuture 클래스 객체를 return 하는 비동기 작업 메소드가 작업이 마쳐지거나 또는 예외가 발생할때
     * 등록된 callback 메소드가 실행이 이루어진다
     *
     * 그러나 막상 실행해보니 동영상과는 다른 결과가 나오게 되는데 실행했을때 로그를 보면 @Aysnc 작업을 위해 생성된 ExecutorService Bean인 applicationTaskExecutor가 listenableFutureRun 메소드의 작업이 끝난뒤 shutdown이 되어버려서
     * 인위적으로 시간을 Delay 시키기 위해 넣은 Thread.sleep 메소드를 실행하는 과정에서 예외가 발생되어 버리는 상황이 벌어졌다.
     * 그럴수밖에 없는 것이 ExecutorService 객체가 shutdown이 되어버리면 현재 ExecutorService 객체에 실행중인 Thread에 어떠한 작업도 더 추가적으로 요청할 수가 없기 때문이다
     * 그래서 이를 확인할려면 Thread.sleep 메소드를 주석처리하면 동영상과 같은 동작을 하게 되는데 그러나 이렇게 할 경우 시간을 지연해서 확인하는 식의 검증 절차를 사용할 수 없기 때문에
     * 우리가 main 메소드에서 설정한 try를 이용한 바로 종료되는 그런 방법이 아니라
     * 일반적인 Spring Boot run 하는 방법(SpringApplication.run(Day04SpringBootApplication.class) 로 실행하는 방법)으로 해서 Embedded WAS가 죽지 않게 함으로써 Spring Boot application이 죽지 않게끔 해주면
     * Thread.sleep 메소드가 실행되더라도 전체적으로 프로세스가 종료되지 않는 상황이기 때문에 ExecutorService 객체가 shutdown 되는 상황이 벌어지지 않아서 확인이 가능하게 된다
     */
    @Bean
    ApplicationRunner listenableFutureRun() {
        return args -> {
            logger.info("listenableFutureRun.run()");
            ListenableFuture<String> f = myService.listenableFuture();
            // ListenableFuture 클래스 객체로 return 하게 한 비동기 작업의 success callback 메소드와 exception callback 메소드를 등록한다
            f.addCallback(s -> logger.info(s), e -> logger.error(e.getMessage()));
            logger.info("listenableFutureRun.exit");
        };
    }

    @RestController
    public static class MyController {

        @GetMapping("/async")
        public String async() {
            return "async";
        }
    }
}
