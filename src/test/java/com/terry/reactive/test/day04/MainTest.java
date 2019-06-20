package com.terry.reactive.test.day04;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class MainTest {

    public static void main(String [] args) {
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(() -> {
            try {
                Thread.sleep(2000);
            }catch(InterruptedException e) {

            }
            logger.info("Hello");
        });
        logger.info("Exit");
    }
}
