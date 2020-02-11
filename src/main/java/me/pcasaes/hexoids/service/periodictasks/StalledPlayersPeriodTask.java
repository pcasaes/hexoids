package me.pcasaes.hexoids.service.periodictasks;

import io.quarkus.scheduler.Scheduled;
import me.pcasaes.hexoids.model.Game;
import me.pcasaes.hexoids.model.Player;
import me.pcasaes.hexoids.service.eventqueue.EventQueueService;
import me.pcasaes.hexoids.service.eventqueue.GameLoopService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class StalledPlayersPeriodTask {

    private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

    StalledPlayersPeriodTask() {
        this.gameLoopService = null;
    }

    @Inject
    public StalledPlayersPeriodTask(EventQueueService<GameLoopService.GameRunnable> gameLoopService) {
        this.gameLoopService = gameLoopService;
    }

    @Scheduled(every = "1m")
    public void checkForStalledPlayers() {
        gameLoopService.enqueue(() -> Game.get().getPlayers()
                .forEach(Player::expungeIfStalled));
    }
}
