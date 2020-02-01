package me.pcasaes.hexoids.util.concurrent.eventqueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple unbounded FIFO event queue that is thread safe only if there's
 * a single thread producing and a single thread consuming. They can be
 * the same thread.
 * <p>
 * This implementation uses a fixed size array
 * <p>
 * The accessor methods will not block but will be subject to weak-consistency.
 *
 * @param <T>
 */
class SingleProducerSingleConsumerFixedArrayEventQueue<T> implements EventQueue<T> {

    private int tail = -1;
    private int prev = -1;

    protected final int maxSizeMinusOne;

    private final List<T> table;

    SingleProducerSingleConsumerFixedArrayEventQueue(int maxSizeExponent) {
        int maxSize = (int) Math.pow(2, maxSizeExponent);
        this.maxSizeMinusOne = maxSize - 1;
        this.table = new ArrayList<>(maxSize);
        for (int i = 0; i < maxSize; i++) {
            table.add(i, null);
        }
    }

    protected int wrapAround(int i) {
        return i & maxSizeMinusOne;
    }

    protected int getNextPosition(int from) {
        return wrapAround(from + 1);
    }

    protected int getNextTail(int tail) {
        return getNextPosition(tail);
    }

    /**
     * Pushes a value to the end of the queue.
     * This method does not block and should only be used by a single thread.
     *
     * @param value
     * @throws IllegalStateException if the queue is full
     */
    @Override
    public void produce(T value) {
        int next;
        tail = next = getNextTail(tail);
        int current = getNextPosition(prev);
        if (next == current && table.get(current) != null) {
            throw new IllegalStateException("Event queue out of space!!! Must be increased");
        }
        table.set(next, value);
    }

    public boolean isEmpty() {
        return prev == tail;
    }

    /**
     * Returns and removes the next item in the queue.
     * <p>
     * Like the access methods this method is weakly consistent.
     *
     * @return the next item in the queue
     */
    @Override
    public T consume() {
        int next = getNextPosition(prev);
        T value = table.get(next);
        if (value != null) {
            prev = next;
            table.set(next, null);
            return value;
        }
        return null;
    }


}
