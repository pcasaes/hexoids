package me.pcasaes.hexoids.core.domain.model.physics;

import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.Player;
import me.pcasaes.hexoids.core.domain.model.Players;
import me.pcasaes.hexoids.core.domain.utils.MathUtil;
import me.pcasaes.hexoids.core.domain.vector.Vector2;

import java.util.function.LongPredicate;

public class Blackhole implements LongPredicate {

    private final EntityId entityId;
    private final float distance;
    private final float impulse;
    private final Vector2 center;
    private final Players players;

    private Blackhole(float x, float y, Players players) {
        this.entityId = EntityId.newId();
        this.center = Vector2.fromXY(x, y);
        this.distance = 0.07F;
        this.impulse = 0.07F;
        this.players = players;
    }

    public static Blackhole massCollapsed(float x, float y, Players players) {
        return new Blackhole(x, y, players);
    }

    @Override
    public boolean test(long l) {
        if (this.players.hasConnectedPlayers()) {
            players.getSpatialIndex()
                    .search(center.getX(), center.getY(), center.getX(), center.getY(), distance)
                    .forEach(this::handleMove);
        }


        return true;
    }

    private float accel(float absMagnitude) {
        float relDistance = absMagnitude / this.distance;
        float invDistance = 1.0F - relDistance;
        return MathUtil.cube(invDistance);
    }

    private void handleMove(Player nearPlayer) {
        if (players.isConnected(nearPlayer.id())) {
            Vector2 distanceBetweenPlayers = center.minus(Vector2
                    .fromXY(nearPlayer.getX(), nearPlayer.getY()));

            float absMagnitude = Math.abs(distanceBetweenPlayers.getMagnitude());

            boolean isNotCenteredNorOutOfRange = absMagnitude < distance && absMagnitude > 0F;
            if (isNotCenteredNorOutOfRange) {

                int sign = distanceBetweenPlayers.getMagnitude() < 0F ? -1 : 1;

                boolean destroyed = absMagnitude <= 0.005;

                if (destroyed) {
                    nearPlayer.hazardDestroy(entityId);
                } else {

                    float acceleration = accel(absMagnitude);

                    Vector2 move = Vector2
                            .fromAngleMagnitude(
                                    distanceBetweenPlayers.getAngle(),
                                    sign * this.impulse * acceleration
                            );

                    nearPlayer.setDampenMovementFactor(1F / (acceleration * 5F + 1F));

                    nearPlayer.move(move.getX(), move.getY(), null);
                }
            }
        }
    }
}
