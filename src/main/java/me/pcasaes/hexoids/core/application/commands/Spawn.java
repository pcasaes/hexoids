package me.pcasaes.hexoids.core.application.commands;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.Game;

public class Spawn {

    private final GameQueue gameQueue;

    Spawn(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void spawn(EntityId userId) {
        this.gameQueue.enqueue(() -> Game.get().getPlayers().createOrGet(userId).spawn());
    }

}
