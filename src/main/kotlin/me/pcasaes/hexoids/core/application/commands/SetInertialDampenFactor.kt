package me.pcasaes.hexoids.core.application.commands

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.Game
import pcasaes.hexoids.proto.SetFixedInertialDampenFactorCommandDto

class SetInertialDampenFactor internal constructor(private val gameQueue: GameQueue) {

    fun setFactor(userId: EntityId, command: SetFixedInertialDampenFactorCommandDto) {
        this.gameQueue.enqueue {
            Game.get().getPlayers()
                .createOrGet(userId)
                .setFixedInertialDampenFactor(command.factor)
        }
    }
}
