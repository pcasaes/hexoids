package me.pcasaes.hexoids.service.eventqueue;

public interface GameQueueService {
    void enqueue(GameLoopService.GameRunnable event);
}
