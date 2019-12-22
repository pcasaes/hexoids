package me.paulo.casaes.bbop.service.eventqueue;

import java.util.function.Consumer;

/**
 * Implementing this will add a new event queue for the specified type.
 *
 * This is used in conjunction with {@link EventQueueService}
 *
 * @param <T>
 */
public interface EventQueueConsumerService<T> extends Consumer<T> {

    /**
     * Called at every check when queue is empty
     */
    void empty();

    /**
     * If the queue is empty will sleep for this specified time.
     * If the returned value is not greater than 0 will not sleep.
     * @return
     */
    long getWaitTime();

    /**
     * If true will use a linked list event queue, otherwise a fixed length array
     * @return
     */
    boolean useLinkedList();

    /**
     * Specifies whether a single thread produces events
     * @return
     */
    boolean useSingleProducer();

    /**
     * Fix length array is an array with size of a power of 2.
     * This specifies the exponent.
     * @return
     */
    int getMaxSizeExponent();

    /**
     * Name of the event queue.
     * @return
     */
    String getName();

    /**
     * The type of the event.
     * @return
     */
    Class<?> getEventType();
}
