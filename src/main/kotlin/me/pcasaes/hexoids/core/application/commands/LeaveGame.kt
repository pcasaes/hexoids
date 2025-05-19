package me.pcasaes.hexoids.core.application.commands;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.Game;
import me.pcasaes.hexoids.core.domain.model.Player;

public class LeaveGame {

    private final GameQueue gameQueue;

    LeaveGame(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void leave(EntityId userId) {
        this.gameQueue.enqueue(() -> Game.get().getPlayers().get(userId).ifPresent(Player::leave));
    }
}
