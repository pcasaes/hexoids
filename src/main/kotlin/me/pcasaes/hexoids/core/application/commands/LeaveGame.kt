package me.pcasaes.hexoids.core.application.commands

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.Game

class LeaveGame internal constructor(private val gameQueue: GameQueue) {

    fun leave(userId: EntityId) {
        this.gameQueue.enqueue {
            Game.get().getPlayers().get(userId)?.leave()
        }
    }
}
