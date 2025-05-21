package me.pcasaes.hexoids.core.domain.model.physics

import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics
import me.pcasaes.hexoids.core.domain.model.Player
import me.pcasaes.hexoids.core.domain.model.Players
import me.pcasaes.hexoids.core.domain.utils.MathUtil
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
        val ratio = 1.0F - MathUtil.square((elapsed / this.duration.toFloat()) - 1F)
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
                .search(center.getX(), center.getY(), center.getX(), center.getY(), dist)
                .forEach { nearPlayer -> handleMove(nearPlayer, dist) }
        }

        return true
    }

    private fun handleMove(nearPlayer: Player, dist: Float) {
        if (nearPlayer !== destroyedPlayer && players.isConnected(nearPlayer.id())) {
            val distanceBetweenPlayers = Vector2
                .fromXY(nearPlayer.getX(), nearPlayer.getY())
                .minus(center)

            val absMagnitude = abs(distanceBetweenPlayers.getMagnitude())

            val isNotCenteredNorOutOfRange = absMagnitude <= dist && absMagnitude > 0F
            if (isNotCenteredNorOutOfRange) {
                val sign = if (distanceBetweenPlayers.getMagnitude() < 0F) -1 else 1

                val move = Vector2
                    .fromAngleMagnitude(
                        distanceBetweenPlayers.getAngle(),
                        sign * this.impulse
                    )

                nearPlayer.move(move.getX(), move.getY(), MoveReason.SHOCKWAVE_PUSH)
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
