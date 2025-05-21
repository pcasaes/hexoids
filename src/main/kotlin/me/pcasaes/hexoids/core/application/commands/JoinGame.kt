package me.pcasaes.hexoids.core.application.commands

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.Game
import pcasaes.hexoids.proto.JoinCommandDto

class JoinGame internal constructor(private val gameQueue: GameQueue) {
    fun join(userId: EntityId, joinCommandDto: JoinCommandDto) {
        this.gameQueue.enqueue {
            val game = Game.get()
            val player = game
                .getPlayers()
                .createPlayer(userId)

            if (player != null) {
                player.join(joinCommandDto)
            } else {
                game.getPlayers().requestCurrentView(userId)
            }

            game.getBolts().requestListOfLiveBolts(userId)
            game.getPlayers().connected(userId)
        }
    }
}
