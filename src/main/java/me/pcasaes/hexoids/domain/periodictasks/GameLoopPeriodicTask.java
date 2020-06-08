package me.pcasaes.hexoids.domain.periodictasks;

import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.service.GameLoopService;

public class GameLoopPeriodicTask implements GamePeriodicTask {

    private final GameQueue gameQueue;

    private final GameLoopService gameLoopService;

    private final long period;

    private GameLoopPeriodicTask(GameQueue gameQueue,
                                 GameLoopService gameLoopService,
                                 long period) {
        this.gameQueue = gameQueue;
        this.gameLoopService = gameLoopService;
        this.period = period;
    }

    public static GameLoopPeriodicTask create(GameQueue gameQueue,
                                              GameLoopService gameLoopService,
                                              long period) {
        return new GameLoopPeriodicTask(gameQueue, gameLoopService, period);
    }

    private void gameLoopPeriodicTask() {
        gameLoopService
                .getFixedUpdateRunnable()
                .ifPresent(this::publish);
    }

    private void publish(Runnable event) {
        this.gameQueue.enqueue(event);
    }

    @Override
    public long getPeriod() {
        return period;
    }

    @Override
    public void run() {
        this.gameLoopPeriodicTask();
    }

}
