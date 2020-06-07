package me.pcasaes.hexoids.domain.eventqueue;

public interface GameQueue {
    void enqueue(Runnable event);
}
