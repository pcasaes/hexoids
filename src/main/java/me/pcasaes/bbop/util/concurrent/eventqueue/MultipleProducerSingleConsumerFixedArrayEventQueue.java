package me.pcasaes.bbop.util.concurrent.eventqueue;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple unbounded FIFO event queue that is thread safe only if there's
 * a single thread consuming. Many threads can produce including the
 * consumer thread.
 *
 * <p>
 * This implementation uses a fixed length array.
 * <p>
 * The accessor methods will not block but will be subject to weak-consistency.
 *
 * @param <T>
 */
class MultipleProducerSingleConsumerFixedArrayEventQueue<T> extends SingleProducerSingleConsumerFixedArrayEventQueue<T> {

    private final AtomicInteger atomicTail = new AtomicInteger(-1);

    MultipleProducerSingleConsumerFixedArrayEventQueue(int maxSizeExponent) {
        super(maxSizeExponent);
    }

    @Override
    protected int getNextTail(int tail) {
        int nextUnwrapped = atomicTail.incrementAndGet();
        int next = super.wrapAround(nextUnwrapped);
        if (next != nextUnwrapped) {
            atomicTail.updateAndGet(this::wrapAround);
        }
        return next;
    }
}