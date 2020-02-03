package me.pcasaes.hexoids.util.concurrent.eventqueue;

/**
 * Simple unbounded FIFO event queue that is thread safe for immutable or effectively immutable
 * objects only if there's a single thread producing and a single thread consuming. They can be
 * the same thread.
 * <p>
 * This implementation uses a linked list.
 * <p>
 * The accessor methods will not block but will be subject to weak-consistency.
 *
 * @param <T>
 */
class SingleProducerSingleConsumerLinkedListEventQueue<T> implements EventQueue<T> {

    private Entry<T> tail;
    private Entry<T> prev;

    /**
     * Pushes a value to the end of the queue.
     * This method does not block and should only be used by a single thread.
     * @param value
     */
    @Override
    public void produce(T value) {
        Entry<T> n = new Entry<>(value);
        if (tail != null) {
            tail.next = n;
            tail = n;
        } else {
            tail = n;
            Entry<T> p = new Entry<>(null);
            p.next = tail;
            prev = p;
        }

    }

    public boolean isEmpty() {
        return prev == tail;
    }

    private boolean isValid(Entry<T> entry) {
        return entry != null && entry.value != null;
    }

    /**
     * Returns and removes the next item in the queue.
     *
     * Like the access methods this method is weakly consistent.
     *
     * @return the next item in the queue
     */
    @Override
    public T consume() {
        if (prev != null && isValid(prev.next)) {
            T r = prev.next.value;
            prev = prev.next;
            prev.value = null;
            return r;
        }
        return null;
    }


    private class Entry<T> {
        // volatile fields allows for safe publishing
        volatile T value; //NOSONAR: This class is specifically used for effectively immutable objects
        Entry<T> next;

        private Entry(T value) {
            this.value = value;
        }
    }
}
