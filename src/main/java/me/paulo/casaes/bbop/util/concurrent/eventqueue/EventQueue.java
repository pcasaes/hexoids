package me.paulo.casaes.bbop.util.concurrent.eventqueue;

/**
 * Simple interface for an event queue
 *
 * @param <T>
 */
public interface EventQueue<T> {

    /**
     * Produces an event
     * @param value
     */
    void produce(T value);

    /**
     * Consumes an event
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
         * See {@link SingleProducerSingleConsumerEventQueue}
         * @return
         */
        public static EventQueue createSingleProducerSingleConsumerEventQueue() {
            return new SingleProducerSingleConsumerEventQueue<>();
        }

        /**
         * See {@link MultipleProducerSingleConsumerEventQueue}
         * @return
         */
        public static EventQueue createMultipleProducerSingleConsumerEventQueue() {
            return new MultipleProducerSingleConsumerEventQueue<>();
        }
    }
}
