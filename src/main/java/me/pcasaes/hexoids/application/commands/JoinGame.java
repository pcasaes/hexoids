package me.pcasaes.hexoids.application.commands;

import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.model.EntityId;
import me.pcasaes.hexoids.domain.model.Game;
import pcasaes.hexoids.proto.JoinCommandDto;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class JoinGame {

    private final GameQueue gameQueue;

    @Inject
    public JoinGame(GameQueue gameQueue) {
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
