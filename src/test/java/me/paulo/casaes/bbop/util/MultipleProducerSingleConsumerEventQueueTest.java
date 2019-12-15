package me.paulo.casaes.bbop.util;

import me.paulo.casaes.bbop.util.concurrent.eventqueue.EventQueue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MultipleProducerSingleConsumerEventQueueTest {

    @Test
    void testOfferProducerThreads() throws Exception {
        final EventQueue<Integer> queue = EventQueue.Factory.createMultipleProducerSingleConsumerEventQueue();

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
        while (i <= total) {
            Integer v = queue.consume();
            if (v == null) {
                continue;
            }
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