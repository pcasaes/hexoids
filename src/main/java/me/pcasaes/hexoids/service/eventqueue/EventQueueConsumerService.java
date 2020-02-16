package me.pcasaes.hexoids.service.eventqueue;

import java.util.function.Consumer;

/**
 * Implementing this will add a new event queue for the specified type.
 * <p>
 * This is used in conjunction with {@link EventQueueSimpleService}
 *
 * @param <T>
 */
public interface EventQueueConsumerService<T> extends Consumer<T> {

    /**
     * Name of the event queue.
     *
     * @return
     */
    String getName();


    default boolean isEnabled() {
        return true;
    }

}
