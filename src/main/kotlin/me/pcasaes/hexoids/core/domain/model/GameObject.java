package me.pcasaes.hexoids.core.domain.model;

import pcasaes.hexoids.proto.ClientPlatforms;
import pcasaes.hexoids.proto.MoveReason;

public interface GameObject {

    void hazardDestroy(EntityId hazardId, long timestamp);

    void move(float moveX, float moveY, MoveReason moveReason);

    float getX();

    float getY();

    default boolean teleport(float x, float y, long timestamp, MoveReason moveReason) {
        return false;
    }

    default ClientPlatforms getClientPlatform() {
        return ClientPlatforms.UNKNOWN;
    }

    default boolean supportsInertialDampener() {
        return false;
    }

    /**
     * Can be used to increase or decrease inertial dampener.
     * At end of next fixed update will reset to 1.
     * <p>
     * Not all game objects support inertial dampener
     *
     * @param factor
     */
    default void setDampenMovementFactorUntilNextFixedUpdate(float factor) {

    }
}
