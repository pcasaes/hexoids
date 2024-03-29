package me.pcasaes.hexoids.core.application.commands;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.Game;
import pcasaes.hexoids.proto.MoveCommandDto;

public class Move {

    private final GameQueue gameQueue;

    Move(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public void move(EntityId userId, MoveCommandDto moveCommandDto) {
        this.gameQueue.enqueue(() -> Game.get().getPlayers()
                .createOrGet(userId)
                .move(moveCommandDto.getMoveX(),
                        moveCommandDto.getMoveY(),
                        moveCommandDto.hasAngle() ? moveCommandDto.getAngle().getValue() : null
                )
        );
    }

}
