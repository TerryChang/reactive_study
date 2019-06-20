package com.terry.reactive.test.day03;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SchedulerMain {
    public static void main(String [] args) {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                boolean canceled = false;
                int no = 0;
                @Override
                public void request(long n) {
                    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
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
    }
}
