package me.pcasaes.hexoids.core.domain.model;

import pcasaes.hexoids.proto.ClientPlatforms;

public interface GameObject {

    void hazardDestroy(EntityId hazardId, long timestamp);

    void move(float moveX, float moveY);

    float getX();

    float getY();

    default ClientPlatforms getClientPlatform() {
        return ClientPlatforms.UNKNOWN;
    }

    default boolean supportsInertialDampener() {
        return false;
    }

    /**
     * Can be used to increase or decrease inertial dampener.
     * At end of next fixed update will reset to 1.
     *
     * Not all game objects support inertial dampener
     * @param factor
     */
    default void setDampenMovementFactorUntilNextFixedUpdate(float factor) {

    }
}
