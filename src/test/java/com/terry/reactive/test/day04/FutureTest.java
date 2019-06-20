package com.terry.reactive.test.day04;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class FutureTest {

    @Test
    public void FutureTest() {
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(() -> {
            try {
                Thread.sleep(2000);
            }catch(InterruptedException e) {

            }
            logger.info("Hello");
        });
        logger.info("Exit");
        // Future는 비동기적인 작업을 수행한뒤 그 결과를 가지고 있는 클래스이다.
        // Future future = null;
    }
}
