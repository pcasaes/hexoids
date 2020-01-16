package me.pcasaes.bbop.util.concurrent.eventqueue;

/**
 * Simple interface for an event queue
 *
 * @param <T>
 */
public interface EventQueue<T> {

    /**
     * Produces an event
     *
     * @param value
     */
    void produce(T value);

    /**
     * Consumes an event
     *
     * @return
     */
    T consume();

    /**
     * @return true if empty
     */
    boolean isEmpty();

    class Factory {

        private Factory() {
        }

        /**
         * See {@link SingleProducerSingleConsumerLinkedListEventQueue}
         *
         * @return
         */
        public static EventQueue createSingleProducerSingleConsumerEventQueue(boolean useLinkedList, int maxSizeExponent) {
            return useLinkedList ? new SingleProducerSingleConsumerLinkedListEventQueue<>() : new SingleProducerSingleConsumerFixedArrayEventQueue<>(maxSizeExponent);
        }

        /**
         * See {@link MultipleProducerSingleConsumerLinkedListEventQueue}
         *
         * @return
         */
        public static EventQueue createMultipleProducerSingleConsumerEventQueue(boolean useLinkedList, int maxSizeExponent) {
            return useLinkedList ? new MultipleProducerSingleConsumerLinkedListEventQueue<>() : new MultipleProducerSingleConsumerFixedArrayEventQueue(maxSizeExponent);
        }
    }
}
