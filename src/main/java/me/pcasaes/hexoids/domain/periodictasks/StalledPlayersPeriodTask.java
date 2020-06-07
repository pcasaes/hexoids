package me.pcasaes.hexoids.domain.periodictasks;

import io.quarkus.scheduler.Scheduled;
import me.pcasaes.hexoids.domain.model.Game;
import me.pcasaes.hexoids.domain.model.Player;
import me.pcasaes.hexoids.domain.eventqueue.GameQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class StalledPlayersPeriodTask {

    private final GameQueue gameLoopService;

    StalledPlayersPeriodTask() {
        this.gameLoopService = null;
    }

    @Inject
    public StalledPlayersPeriodTask(GameQueue gameLoopService) {
        this.gameLoopService = gameLoopService;
    }

    @Scheduled(every = "1m")
    public void checkForStalledPlayers() {
        gameLoopService.enqueue(() -> Game.get().getPlayers()
                .forEach(Player::expungeIfStalled));
    }
}
