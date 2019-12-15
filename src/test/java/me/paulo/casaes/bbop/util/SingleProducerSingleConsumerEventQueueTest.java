package me.paulo.casaes.bbop.util;

import me.paulo.casaes.bbop.util.concurrent.eventqueue.EventQueue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleProducerSingleConsumerEventQueueTest {

    @Test
    void testPopNewEmpty() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue();

        assertNull(queue.consume());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testOffer1() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue();

        queue.produce(1);
        assertFalse(queue.isEmpty());

        assertEquals(Integer.valueOf(1), queue.consume());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testOffer1_2_3_4_5_Full() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue();

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

    @Test
    void testOfferProducerThread() throws Exception {
        final EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue();

        final int total = 100000;
        final CountDownLatch latch = new CountDownLatch(total);
        new Thread(() -> {
            for (int i = 1; i <= total; i++) {
                queue.produce(i);
                latch.countDown();
            }
        }).start();


        int i = 1;
        while (i <= total) {
            Integer v = queue.consume();
            if (v == null) {
                continue;
            }
            assertEquals(Integer.valueOf(i), v);
            i++;
        }
        assertTrue(queue.isEmpty());

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}