package me.pcasaes.hexoids.application.commands;

import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.model.EntityId;
import me.pcasaes.hexoids.domain.model.Game;

public class Fire {

    private final GameQueue gameQueue;

    Fire(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void fire(EntityId userId) {
        this.gameQueue.enqueue(() -> Game.get().getPlayers().createOrGet(userId).fire());
    }

}
