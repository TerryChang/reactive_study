package com.terry.reactive.test;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObTest {

    @Test
    public void obTest1() {
        Iterable<Integer> iter = () ->
           new Iterator<Integer>() {
               int i = 1;
               final static int MAX = 10;

               public boolean hasNext() {
                   return i < MAX;
               }

               public Integer next() {
                   return i++;
               }
           };


        /**
         * java의 for each는 Iterator 인터페이스 구현 객체를 return 하는 Iterable 인터페이스를 구현한 객체를 넣으면 된다
         */
        for(Integer i : iter) {
            System.out.println(i);
        }

        /**
         * Java 5 이전 버전일때
         */
        for(Iterator<Integer> i = iter.iterator(); i.hasNext(); ) {
            System.out.println("oldest version : " + i.next());
        }
    }

    @Test
    public void obTest2() {
        // observer(subscribe)
        /*Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println(arg);
            }
        };*/
        Observer ob = (o, arg) -> {
            System.out.println(Thread.currentThread().getName() + " " + arg);
        };



        IntObservable intObservable = new IntObservable();
        intObservable.addObserver(ob);
        // intObservable.run();

        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(intObservable);
        System.out.println(Thread.currentThread().getName() + " EXIT");
        es.shutdown();
    }

    // observable(publisher)
    static class IntObservable extends Observable implements Runnable {
        @Override
        public void run() {
            for(int i = 1; i <= 10; i++) {
                setChanged();
                notifyObservers(i);
            }
        }
    }
}
