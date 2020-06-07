package me.pcasaes.hexoids.application.commands;

import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.model.EntityId;
import me.pcasaes.hexoids.domain.model.Game;
import pcasaes.hexoids.proto.MoveCommandDto;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class Move {

    private final GameQueue gameQueue;

    @Inject
    public Move(GameQueue gameQueue) {
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
