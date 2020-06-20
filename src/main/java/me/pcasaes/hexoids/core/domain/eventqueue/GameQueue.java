package me.pcasaes.hexoids.core.domain.eventqueue;

/**
 * This interface is used to run events in the game loop.
 * The game model expects to run in a single thread. An asynchronous single threaded
 * infrastructure is expected to be provided for this and there this interface will
 * be implemented.
 */
public interface GameQueue {
    void enqueue(Runnable event);
}
