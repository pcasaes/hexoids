package me.pcasaes.hexoids.core.domain.periodictasks;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.Game;
import me.pcasaes.hexoids.core.domain.model.Player;

import java.util.concurrent.TimeUnit;

public class StalledPlayersPeriodTask implements GamePeriodicTask {

    private final GameQueue gameQueue;

    private StalledPlayersPeriodTask(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public static StalledPlayersPeriodTask create(GameQueue gameQueue) {
        return new StalledPlayersPeriodTask(gameQueue);
    }


    @Override
    public long getPeriod() {
        return 60;
    }

    @Override
    public void run() {
        gameQueue.enqueue(() -> Game.get().getPlayers()
                .forEach(Player::expungeIfStalled));
    }

    @Override
    public long getDelay() {
        return 0;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }
}
