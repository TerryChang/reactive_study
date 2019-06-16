package com.terry.reactive.test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PubSub02 {

  /*
  지난 첫번째 강의 복습 타이밍
  Publisher : 데이터를 발생하는것
  Subscriber : 데이터를 받는것
  Subscription : 둘 사이에 실제 구독이 한번 일어나는 액션을 담고 있는것
  
   */
  @Test
  public void PublisherSubscriber복습() {
    
    Publisher<Integer> pub = new Publisher<Integer>() {
      /*
       이번 회차에서는 데이터 생성하는 방법을 Stream 클래스를 이용해서 구현했다.
       iterate는 계속 데이터를 생성하는 메소드인데 첫번째 파라미터로는 데이터의 시작값을 설정한다(여기서는 1부터 5까지 생성하는 것이기 때문에 1을 준다)
       두번째 파라미터는 다음 단계의 데이터를 생성하는 함수를 정의하는 것이다. javadoc으로 iterator 메소드의 정의를 살펴보면 두번째 파라미터는 UnaryOperator<T>로 정의되어 있다
       UnaryOperator 타입은 함수형 인터페이스로 파라미터로 받은 값의 타입과 리턴되는 값의 타입이 동일한 함수형 인터페이스이다.
       그래서 이 부분을 보면 a->a+1로 되어 있는데 이 얘기는 파라미터로 받은 a에 1을 더한값을 return 한다는 것이 된다.
       이것이 계속 반복적으로 이루어지는 것이다. a에 1을 더한 값을 다시 입력 파라미터로 잡고 a+1을 다시 태워서 리턴하고 또 그 리턴된 값을 다시 입력 파라미터로 잡고..
       이렇게 하면 1,2,3,4,5...이렇게 계속 1부터 시작되는 1씩 증가한 값을 생성할수 있다.
       그러나 iterate 메소드를 이렇게만 하고 마무리 지으면 계속 무한대로 생성할려고 시도하기 때문에 끝이 나지 않게 된다.
       그래서 여기서 사용하는 것이 limit 메소드이다. limit 메소드에 들어가는 파라미터는 생성해야 할 데이터의 갯수이다. 현재 예제에서는 1씩 더하기 때문에 마지막 값과 갯수가 같아지는 상황이 벌어져서 마지막 값으로 오해할 소지가 있겠으나..
       limit 는 생성해야 할 데이터 갯수를 지정하는 것이다. iterate의 return 타입이 Stream 타입이기 때문에 chain 방식으로의 호출이 가능하다.
       그리고 collect(Collectors.toList()) 를 이용해서 만들어진 데이터들을 List 인터페이스에 담는다
       List 인터페이스는 Iterable 인터페이스의 하위 인터페이스이기 때문에 Iterable 인터페이스로 받을수가 있다
       */
      Iterable<Integer> iter = Stream.iterate(1, a->a+1).limit(10).collect(Collectors.toList());
      @Override
      public void subscribe(Subscriber<? super Integer> sub) {
        // subscribe 메소드에서는 Subscriber의 onSubscribe 메소드를 호출해야 한다
        // TODO Auto-generated method stub
        sub.onSubscribe(new Subscription() {

          @Override
          public void request(long n) {
            // TODO Auto-generated method stub
            
            try {
              // 첫번째 강의때는 while 문과 iterator 객체의 hasnext, next 메소드를 이용해서 각각의 값을 꺼내서 Subscriber의 onNext를 호출하는게 아니라
              // Iterator 인터페이스의 forEach 문으로 하나씩 꺼내서 onNext메소드에 전달하고 있다 
              iter.forEach(s -> sub.onNext(s));
              sub.onComplete();                 // 전송이 완료되었다고 신호를 준다
            } catch (Throwable t) {
              sub.onError(t);
            }
          }

          @Override
          public void cancel() {
            // TODO Auto-generated method stub
            
          }
          
        });
      }
      
    };
    
    Subscriber<Integer> sub = new Subscriber<Integer>() {

      @Override
      public void onSubscribe(Subscription s) {
        // TODO Auto-generated method stub
        logger.debug("onSubscribe");
        s.request(Long.MAX_VALUE); // Publisher가 가지고 있는 데이터를 전부 받겠다(분할로 받는 것이 아님)
      }

      @Override
      public void onNext(Integer i) {
        // TODO Auto-generated method stub
        logger.debug("onNext : {}", i);
        
      }

      @Override
      public void onError(Throwable t) {
        // TODO Auto-generated method stub
        logger.debug("onError : {}", t);
      }

      @Override
      public void onComplete() {
        // TODO Auto-generated method stub
        logger.debug("onComplete");
      }
      
    };
    
    pub.subscribe(sub);
    
  }
  
  /**
   * 스프링 리액티브 프로그래밍(2) - Reactive Streams - Operation 동영상에서 위의 복습용 테스트 메소드인 PublisherSubscriber복습 메소드를 Refactoring 한 메소드이다
   * Publisher 클래스 객체를 생성하는 부분과 Subscriber 클래스 객체를 생성하는 부분을
   * 각각 iterPub 메소드와 logSub 메소드로 별도 메소드로 분리해서 Refactoring을 했다
   * 
   * Operator를 본격적으로 설명하면서 사용된 테스트 메소드
   * 
   * Reactive Streams - Operators
   * 
   * Publisher -> (Data1) -> Operator1 -> (Data2) -> Operator2 -> (Data3) -> Subscriber
   * 
   * Publisher에서 Subscriber로 Data1을 전송할때 Data1은 Operator1을 거쳐서 Data2로 변환되고 이 Data2는 Operator2를 거쳐서 Data3가 되고 이 Data3가 Subscriber 에세 전달이 되는 과정을 진행해본다
   * 이 변환에 사용되는 Operator1과 Operator2가 이번 강의에서 설명하는 Operator가 된다
   * 
   * 1. map (data1 -> function -> data2)
   * pub -> (Data1) -> mapPub -> (Data2) -> logSub
   *                 <- subscribe(logSub) : 데이터가 Publisher 쪽으로 흘러간다(Subscriber 객체가 Publisher 쪽으로 등록이 되기 때문에)
   *                 -> onSubscribe(s)
   *                 -> onNext
   *                 -> onNext
   *                 -> onComplete
   *                 
   * publisher에서 subscriber로 데이터가 흘러가는것을 downstream(위에서 아래로 흘러가니까)이라 하고
   * subscriber에서 publisher로 데이터가 흘러가는것을 upstream이라 한다
   * 
   * Function Interface는 Generic Type을 2개를 받는데 어떤 타입의 데이터를 받아 어떤 타입의 데이터로 return 하면 되는지를 정의하는 것이다
   * 그래서 2개의 타입을 받는 것이다.
   * Function Interface의 소스를 보면 R apply(T t) 란 메소드가 있는데 이것이 T 타입의 파라미터를 받아 R 타입의 데이터로 return 한다는 것이 된다
   * 그래서 (Function<Integer, Integer>)s -> s * 10 를 사용함으로써 apply 메소드를 람다식으로 구현한것이다(s를 받아서 s * 10 값을 return 하는 것이다)
   * 이런 이유로 parameter 타입도 Integer, return 타입도 Integer로 설정하는 것이다
   */
  @Test
  public void PublisherSubscriber2() {
    Publisher<Integer> pub = iterPub(Stream.iterate(1, a->a+1).limit(10).collect(Collectors.toList()) );
    // Publisher<Integer> mapPub = mapPub(pub, s -> s * 10);
    // mapPub.subscribe(logSub());
    
    // 여기서 다음과 같이 mapPub2를 하나 더 만들어서 거치게도 할 수 있다
    // 이렇게 하면 mapPub을 통해 10이 곱해진 값을 음수로 변환해서 return 하게 된다
    // pub -> (Data1 : 1 부터 10까지의 값) -> mapPub -> (Data2 : Data1 * 10) -> mapPub2 -> (Data3 : -(Data2)) -> logSub
    // Publisher<Integer> mapPub2 = mapPub(mapPub, s -> -s);
    // mapPub2.subscribe(logSub());
    
    // 어떤 Publisher 객체를 파라미터로 넣어주면 해당 Publisher가 던져주는 값들의 합계를 계산한 결과값을 제공하는 Publisher 객체를 만드는 작업을 한다
    // 이것은 위의 mapPub과는 동작에 있어서 약간 다른 점이 있다
    // mapPub의 경우는 원래 제공하는 Publisher에서 데이터가 10개가 넘어오면 해당 데이터를 원하는 데이터로 맞게끔 변환하는 과정을 거쳐서 원래 Publisher가 제공하는 데이터와 동일한 개수의 데이터가 넘어오지만
    // 지금부터 구현할 subPub의 경우는 데이터가 10개 넘어오면 그 10개를 모두 던지는게 아니라 해당 데이터들을 모두 더한값을 가지고 있다가 Subscriber 객체가 구독할때 그 시점에서 모두 더한 값을 전달해주기 때문에 동일한 개수의 데이터가 전달되는 것이 아니기 때문이다
    
    Publisher<Integer> sumPub = sumPub(pub);
    sumPub.subscribe(logSub());
    
  }

  
  // sumPub 메소드를 살펴보면 mapPub 메소드와 같은 방식의 코드를 사용하고 있다.
  // 그러나 mapPub과는 다른점이 있다.
  // Publisher에서 다음 값을 파라미터로 받는 onNext 메소드에서 파라미터로 받은 값들을 sum 하는 작업을 진행해야 한다.
  // 이를 위해 DelegateSub 클래스 객체를 만들때 멤버변수로 sum을 만들고
  // onNext 메소드에서 파라미터로 받은 값들을 sum 변수에 계속 더하고 있다.
  // 그러나 이 시점에서 logSub 메소드가 만든 Subscriber 객체의 onNext 메소드를 호출할수는 없다. 
  // 왜냐면 값을 더하는 작업이 모두 끝난뒤에 logSub 메소드가 만든 Subscriber 객체의 onNext 메소드를 호출해야 모두 더한 값이 전달되기 때문이다
  // 그러면 어느 시점에 logSub 메소드가 만든 Subscriber 객체의 onNext 메소드를 호출하느냐..
  // 데이터를 넘겨주는 작업이 모두 끝나면 onComplete 메소드를 호출하게 되는데..
  // 바로 이 onComplete 메소드를 호출할때 그때 logSub 메소드가 만든 Subscriber 객체의 onNext 메소드를 호출하여 최종 합계값을 던져주게 된다(최종 결과값 한번만 넘겨준다)
  // 그리고 최종 합계값을 던져주는걸로 작업이 완료되었기 때문에 logSub 메소드가 만든 Subscriber 객체의 onComplete 메소드를 호출하여 작업이 모두 끝났다는 것을 알려준다
   
  private Publisher<Integer> sumPub(Publisher<Integer> pub) {
    // TODO Auto-generated method stub
    return new Publisher<Integer>() {

      @Override
      public void subscribe(Subscriber<? super Integer> sub) {
        // TODO Auto-generated method stub
        pub.subscribe(new DelegateSub(sub) {
           int sum = 0;
          @Override
          public void onNext(Integer i) {
            // TODO Auto-generated method stub
            sum += i;
          }
          @Override
          public void onComplete() {
            // TODO Auto-generated method stub
            sub.onNext(sum);
            sub.onComplete();
          }
          
        });
      }
      
    };
  }

  private Publisher<Integer> mapPub(Publisher<Integer> pub, Function<Integer, Integer> f) {
    // TODO Auto-generated method stub
    return new Publisher<Integer>() {

      // subscribe 메소드의 sub는 PublisherSubscriber2의 mapPub.subscribe(logSub()); 코드에서 logSub() 으로 받게되는 원래 Subscriber 객체이다.
      @Override
      public void subscribe(Subscriber<? super Integer> sub) {
        // TODO Auto-generated method stub
        // mapPub 메소드가 파라미터로 받은 pub의 subscribe 메소드를 Subscriber 객체가 호출하게 함으로써 
        // 실질적으로 Subscriber 객체가 pub의 subscribe 메소드를 호출하게끔 한다 
        // pub -> mapPub -> logSub 의 구조가 완성된다
        // 잘 이해가 안되면 PublisherSubscriber2의 mapPub.subscribe(logSub()); 코드부터 호출관계를 역으로 추적해보자
        
        // pub.subscribe(sub);
        
        // 그러나 위와 같이 pub.subscribe(sub) 식으로 코딩을 마무리해버리면 원래 의도인 10을 곱해서 return 하는 기능을 수행할수 없기 때문에
        // 이 부분을 진행하기 위해 파라미터로 받은 Function 인터페이스가 기능을 수행하여 만든 값을 return 하는 기능을 갖고 있는 새로운 Subscriber 객체를 생성하게 된다
        /*
        pub.subscribe(new Subscriber<Integer>() {

          @Override
          public void onSubscribe(Subscription s) {
            // TODO Auto-generated method stub
            // subscribe 메소드에서 파라미터로 받은 Subscriber 객체의 onSubscribe 메소드를 호출하는 방식으로 중계해주는 역할만 한다
            sub.onSubscribe(s);
          }

          @Override
          public void onNext(Integer t) {
            // TODO Auto-generated method stub
            // mapPub 메소드 정의에서 두번째 파라미터로 받은 Function 인터페이스의 apply 메소드를 사용한뒤 전달함으로써
            // 넘겨받은 Function 인터페이스 구현 객체의 기능(곱하기 10, 음수 변환 등)의 값을 subscribe 메소드의 sub의 onNext 메소드에 전달해준다
            sub.onNext(f.apply(t));
          }

          @Override
          public void onError(Throwable t) {
            // TODO Auto-generated method stub
            // onSubscribe 메소드같이 중계해주는 역할만 한다
            sub.onError(t);
          }

          @Override
          public void onComplete() {
            // TODO Auto-generated method stub
            // onSubscribe 메소드같이 중계해주는 역할만 한다
            sub.onComplete();
          }
          
        });
        */
        
        // 아래의 코드는 위에 주석으로 처리한 코드에서 new Subscriber<Integer>() {...} 부분을 별도 클래스인 DelegateSub 클래스로 별도로 빼서 구현한 것이다.
        // 이렇게 한 이유는 onNext 메소드를 제외한 나머지 onSubscribe, onError, onComplete 메소드는
        // 위의 코드에서 subscribe 메소드로 전달받은 파라미터인 Subscriber<? super Integer> sub 객체의 onSubscribe, onError, onComplete 메소드를 각각 실행하는 것이기 때문에
        // Subscriber 클래스 객체를 멤버변수로 하는 Subscriber<Integer> 인터페이스를 구현하는 클래스를 별도로 만든뒤에
        // 여기에 생성자로 Subscriber 클래스 객체를 넘겨주게 함으로써..
        // Subscriber<? super Integer> sub 객체를 새로운 클래스의 Subscriber 클래스 객체를 멤버변수에 설정되게 하고
        // 새로운 클래스의 onSubscribe, onError, onComplete 메소드에서 위에서 설정한 Subscriber 클래스 객체 멤버변수의 onSubscribe, onError, onComplete 메소드를 호출하게 힌다
        // onNext를 제외한 나머지 onSubscribe, onError, onComplete 메소드는 실제 구현되는 코드가 인계받은 Subscriber<? super Integer> sub 객체의 onSubscribe, onError, onComplete 메소드를 대신 호출해주는 역할 외에는 바뀔일이 없기 때문에
        // 이렇게 새로운 클래스를 만든뒤에 onNext 메소드만 override 해서 별도로 구현하게 하는 것이다
        pub.subscribe(new DelegateSub(sub) {

          @Override
          public void onNext(Integer i) {
            // TODO Auto-generated method stub
            sub.onNext(f.apply(i));
          }
          
        });
        
      }
      
    };
  }

  /**
   * PublisherSubscriber복습 Test Method에서 Publisher 객체 생성하는 부분을 별도 메소드로 추출
   * @return
   */
  private Publisher<Integer> iterPub(List<Integer> iter) {
    return new Publisher<Integer>() {
      
      @Override
      public void subscribe(Subscriber<? super Integer> sub) {
        // TODO Auto-generated method stub
        sub.onSubscribe(new Subscription() {

          @Override
          public void request(long n) {
            // TODO Auto-generated method stub
            
            try {
              iter.forEach(s -> sub.onNext(s));
              sub.onComplete();                 
            } catch (Throwable t) {
              sub.onError(t);
            }
          }

          @Override
          public void cancel() {
            // TODO Auto-generated method stub
            
          }
          
        });
      }
      
    };
  }
  
  /**
   * PublisherSubscriber복습 Test Method에서 Subscriber 객체 생성하는 부분을 별도 메소드로 추출
   * @return
   */
  private Subscriber<Integer> logSub() {
    return new Subscriber<Integer>() {

      @Override
      public void onSubscribe(Subscription s) {
        // TODO Auto-generated method stub
        logger.debug("onSubscribe");
        s.request(Long.MAX_VALUE); // Publisher가 가지고 있는 데이터를 전부 받겠다(분할로 받는 것이 아님)
      }

      @Override
      public void onNext(Integer i) {
        // TODO Auto-generated method stub
        logger.debug("onNext : {}", i);
        
      }

      @Override
      public void onError(Throwable t) {
        // TODO Auto-generated method stub
        logger.debug("onError : {}", t);
      }

      @Override
      public void onComplete() {
        // TODO Auto-generated method stub
        logger.debug("onComplete");
      }
      
    };
  }
}
