package me.pcasaes.bbop.util.concurrent.eventqueue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SingleProducerSingleConsumerFixedArrayEventQueueTest {

    @Test()
    void testMaxSizePassedNeverConsume() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(false, 1);

        queue.produce(1);
        queue.produce(2);
        assertThrows(IllegalStateException.class, () -> queue.produce(3));
    }

    @Test()
    void testMaxSizePassedAfterConsume() {
        EventQueue<Integer> queue = EventQueue.Factory.createSingleProducerSingleConsumerEventQueue(false, 1);

        queue.produce(1);
        queue.produce(2);
        assertEquals(1, queue.consume().intValue());
        queue.produce(3);
        assertThrows(IllegalStateException.class, () -> queue.produce(4));
    }
}