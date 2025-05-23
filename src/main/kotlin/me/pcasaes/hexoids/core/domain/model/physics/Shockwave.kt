package me.pcasaes.hexoids.core.domain.model.physics

import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics
import me.pcasaes.hexoids.core.domain.model.Player
import me.pcasaes.hexoids.core.domain.model.Players
import me.pcasaes.hexoids.core.domain.utils.square
import me.pcasaes.hexoids.core.domain.vector.Vector2
import pcasaes.hexoids.proto.MoveReason
import java.util.function.LongPredicate
import kotlin.math.abs

class Shockwave private constructor(
    private val destroyedPlayer: Player,
    private val players: Players,
    private val startedAt: Long
) : LongPredicate {

    private val center: Vector2 = Vector2.fromXY(destroyedPlayer.getX(), destroyedPlayer.getY())

    private val duration: Long = Config.getPlayerDestroyedShockwave().getDuration()
    private val durationWithPadding: Long = this.duration + 20
    private val distance: Float = Config.getPlayerDestroyedShockwave().getDistance()
    private val impulse: Float = Config.getPlayerDestroyedShockwave().getImpulse()

    private fun range(elapsed: Long): Float {
        val ratio = 1.0F - ((elapsed / this.duration.toFloat()) - 1F).square()
        return this.distance * ratio
    }

    override fun test(timestamp: Long): Boolean {
        val elapsed = timestamp - this.startedAt
        if (elapsed > this.durationWithPadding) {
            return false
        }

        if (this.players.getNumberOfConnectedPlayers() > 0 && elapsed > 0) {
            val dist = range(elapsed)

            players.getSpatialIndex()
                .search(center.x, center.y, center.x, center.y, dist)
                .forEach { nearPlayer -> handleMove(nearPlayer, dist) }
        }

        return true
    }

    private fun handleMove(nearPlayer: Player, dist: Float) {
        if (nearPlayer !== destroyedPlayer && players.isConnected(nearPlayer.id())) {
            val distanceBetweenPlayers = Vector2
                .fromXY(nearPlayer.getX(), nearPlayer.getY())
                .minus(center)

            val absMagnitude = abs(distanceBetweenPlayers.magnitude)

            val isNotCenteredNorOutOfRange = absMagnitude <= dist && absMagnitude > 0F
            if (isNotCenteredNorOutOfRange) {
                val sign = if (distanceBetweenPlayers.magnitude < 0F) -1 else 1

                val move = Vector2
                    .fromAngleMagnitude(
                        distanceBetweenPlayers.angle,
                        sign * this.impulse
                    )

                nearPlayer.move(move.x, move.y, MoveReason.SHOCKWAVE_PUSH)
                GameMetrics.getMovedByShockwave().increment(nearPlayer.getClientPlatform())
            }
        }
    }

    companion object {
        fun shipExploded(fromPlayer: Player, players: Players, startedAt: Long): Shockwave {
            return Shockwave(fromPlayer, players, startedAt)
        }
    }
}
