package me.pcasaes.hexoids.core.application.commands

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.Game
import pcasaes.hexoids.proto.MoveCommandDto

class Move internal constructor(private val gameQueue: GameQueue) {

    fun move(userId: EntityId, moveCommandDto: MoveCommandDto) {
        this.gameQueue.enqueue {
            Game.get()
                .getPlayers()
                .createOrGet(userId)
                .move(
                    moveCommandDto.moveX,
                    moveCommandDto.moveY,
                    if (moveCommandDto.hasAngle()) moveCommandDto.angle.value else null
                )
        }
    }
}
