package me.paulo.casaes.bbop.util.concurrent.eventqueue;

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


    MultipleProducerSingleConsumerFixedArrayEventQueue(int maxSizeExponent) {
        super(maxSizeExponent);
    }

    /**
     * Pushes a value to the end of the queue.
     * This method does block and is thread safe.
     *
     * @param value
     * @throws IllegalStateException if the queue is full
     */
    @Override
    public synchronized void produce(T value) { //NOSONAR: false positive. This method blocks
        super.produce(value);
    }
}
