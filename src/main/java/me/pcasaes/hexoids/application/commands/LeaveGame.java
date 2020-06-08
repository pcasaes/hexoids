package me.pcasaes.hexoids.application.commands;

import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.model.EntityId;
import me.pcasaes.hexoids.domain.model.Game;
import me.pcasaes.hexoids.domain.model.Player;

public class LeaveGame {

    private final GameQueue gameQueue;

    LeaveGame(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void leave(EntityId userId) {
        this.gameQueue.enqueue(() -> Game.get().getPlayers().get(userId).ifPresent(Player::leave));
    }
}
