package me.pcasaes.hexoids.core.application.commands

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.Game

class Fire internal constructor(private val gameQueue: GameQueue) {

    fun fire(userId: EntityId) {
        this.gameQueue.enqueue { Game.get().getPlayers().createOrGet(userId).fire() }
    }
}
