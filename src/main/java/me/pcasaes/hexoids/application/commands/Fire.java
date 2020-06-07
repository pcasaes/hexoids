package me.pcasaes.hexoids.application.commands;

import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.model.EntityId;
import me.pcasaes.hexoids.domain.model.Game;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class Fire {

    private final GameQueue gameQueue;

    @Inject
    public Fire(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void fire(EntityId userId) {
        this.gameQueue.enqueue(() -> Game.get().getPlayers().createOrGet(userId).fire());
    }

}
