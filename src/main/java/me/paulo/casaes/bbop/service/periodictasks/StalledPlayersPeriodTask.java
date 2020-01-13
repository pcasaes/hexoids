package me.paulo.casaes.bbop.service.periodictasks;

import io.quarkus.scheduler.Scheduled;
import me.paulo.casaes.bbop.model.Game;
import me.paulo.casaes.bbop.model.Player;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StalledPlayersPeriodTask {

    @Scheduled(every = "1m")
    public void checkForStalledPlayers() {
        Game.get().getPlayers()
                .stream()
                .forEach(Player::expungeIfStalled);
    }
}
