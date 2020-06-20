package me.pcasaes.hexoids.core.application.commands;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.Game;
import pcasaes.hexoids.proto.JoinCommandDto;

public class JoinGame {

    private final GameQueue gameQueue;

    JoinGame(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void join(EntityId userId, JoinCommandDto joinCommandDto) {
        this.gameQueue.enqueue(() -> {
            Game.get()
                    .getPlayers()
                    .createPlayer(userId)
                    .ifPresentOrElse(
                            player -> player.join(joinCommandDto),
                            () -> Game.get().getPlayers().requestListOfPlayers(userId)
                    );

            Game.get().getBolts().requestListOfLiveBolts(userId);
            Game.get().getPlayers().connected(userId);
        });
    }
}
