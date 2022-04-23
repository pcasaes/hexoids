package me.pcasaes.hexoids.core.application.commands;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.Game;
import pcasaes.hexoids.proto.SetFixedInertialDampenFactorCommandDto;

public class SetInertialDampenFactor {

    private final GameQueue gameQueue;

    SetInertialDampenFactor(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void setFactor(EntityId userId, SetFixedInertialDampenFactorCommandDto command) {
        this.gameQueue.enqueue(() -> Game.get().getPlayers()
                .createOrGet(userId)
                .setFixedInertialDampenFactor(command.getFactor())
        );
    }
}
