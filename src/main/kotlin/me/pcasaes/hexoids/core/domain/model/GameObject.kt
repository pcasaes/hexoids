package me.pcasaes.hexoids.core.domain.model

import pcasaes.hexoids.proto.ClientPlatforms
import pcasaes.hexoids.proto.MoveReason

interface GameObject {
    fun hazardDestroy(hazardId: EntityId, timestamp: Long)

    fun move(moveX: Float, moveY: Float, moveReason: MoveReason?)

    fun getX(): Float

    fun getY(): Float

    fun teleport(x: Float, y: Float, timestamp: Long, moveReason: MoveReason): Boolean {
        return false
    }

    fun getClientPlatform(): ClientPlatforms {
        return ClientPlatforms.UNKNOWN
    }

    fun supportsInertialDampener(): Boolean {
        return false
    }

    /**
     * Can be used to increase or decrease inertial dampener.
     * At end of next fixed update will reset to 1.
     *
     *
     * Not all game objects support inertial dampener
     *
     * @param factor
     */
    fun setDampenMovementFactorUntilNextFixedUpdate(factor: Float) {
    }
}
