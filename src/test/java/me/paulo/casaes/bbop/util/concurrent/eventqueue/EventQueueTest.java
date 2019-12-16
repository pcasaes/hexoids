package me.paulo.casaes.bbop.util.concurrent.eventqueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventQueueTest {

    private boolean useLinkedList;

    @BeforeEach
    void setup(RepetitionInfo repetitionInfo) {
        useLinkedList = repetitionInfo.getCurrentRepetition() == 1;
    }

    @RepeatedTest(2)
    void testPopNewEmpty() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(useLinkedList, 17);

        assertNull(queue.consume());
        assertTrue(queue.isEmpty());
    }

    @RepeatedTest(2)
    void testOffer1() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(useLinkedList, 17);

        queue.produce(1);
        assertFalse(queue.isEmpty());

        assertEquals(Integer.valueOf(1), queue.consume());
        assertTrue(queue.isEmpty());
    }

    @RepeatedTest(2)
    void testOffer1_2_3_4_5_Full() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(useLinkedList, 17);

        for (int i = 1; i <= 5; i++) {
            queue.produce(i);
            assertFalse(queue.isEmpty());
        }

        for (int i = 1; i <= 5; i++) {
            assertEquals(Integer.valueOf(i), queue.consume());
            if (i < 5) {
                assertFalse(queue.isEmpty());
            }
        }
        assertTrue(queue.isEmpty());
    }

    @RepeatedTest(2)
    void testOfferProducerThread() throws Exception {
        final EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(useLinkedList, 17);

        final int total = 100000;
        final CountDownLatch latch = new CountDownLatch(total);
        new Thread(() -> {
            for (int i = 1; i <= total; i++) {
                queue.produce(i);
                latch.countDown();
            }
        }).start();


        int i = 1;
        int breakout = 0;
        while (i <= total) {
            Integer v = queue.consume();
            if (v == null) {
                assertTrue(breakout++ < 100000);
                Thread.yield();
                continue;
            }
            breakout = 0;
            assertEquals(Integer.valueOf(i), v);
            i++;
        }
        assertTrue(queue.isEmpty());

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }


    @RepeatedTest(2)
    void testOfferProducerThreads() throws Exception {
        final EventQueue<Integer> queue = EventQueue.Factory.createMultipleProducerSingleConsumerEventQueue(useLinkedList, 17);

        final int total = 100000;
        final int half_of_total = total / 2;
        final CountDownLatch latch = new CountDownLatch(total);

        final CountDownLatch startBarrier = new CountDownLatch(2);

        Thread thread1 = new Thread(() -> {
            startBarrier.countDown();
            try {
                startBarrier.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 1; i <= total; i += 2) {
                queue.produce(i);
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            startBarrier.countDown();
            try {
                startBarrier.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 2; i <= total; i += 2) {
                queue.produce(i);
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();


        int i = 1;
        List<Integer> odd = new ArrayList<>(half_of_total);
        List<Integer> even = new ArrayList<>(half_of_total);
        int breakout = 0;
        while (i <= total) {
            Integer v = queue.consume();
            if (v == null) {
                assertTrue(breakout++ < 100000);
                Thread.yield();
                continue;
            }
            breakout = 0;
            if (v % 2 == 0) {
                even.add(v);
            } else {
                odd.add(v);
            }
            i++;
        }
        assertTrue(queue.isEmpty());

        i = 2;
        for (Integer v : even) {
            assertEquals(i, v.intValue());
            i += 2;
        }
        assertEquals(total + 2, i);

        i = 1;
        for (Integer v : odd) {
            assertEquals(i, v.intValue());
            i += 2;
        }
        assertEquals(total + 1, i);


        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

}