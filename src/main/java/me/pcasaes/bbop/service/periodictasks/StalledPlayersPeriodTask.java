package me.pcasaes.bbop.service.periodictasks;

import io.quarkus.scheduler.Scheduled;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.model.Player;

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
