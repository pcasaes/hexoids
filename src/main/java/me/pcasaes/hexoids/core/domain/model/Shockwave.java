package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics;
import me.pcasaes.hexoids.core.domain.utils.MathUtil;
import me.pcasaes.hexoids.core.domain.vector.Vector2;

import java.util.function.LongPredicate;

public class Shockwave implements LongPredicate {


    private final Player fromPlayer;
    private final Vector2 center;
    private final Players players;
    private final long startedAt;

    private final long duration;
    private final long durationWithPadding;
    private final float distance;
    private final float impulse;


    private Shockwave(Player fromPlayer, Players players, long startedAt) {
        this.fromPlayer = fromPlayer;
        this.players = players;
        this.startedAt = startedAt;
        this.center = Vector2.fromXY(fromPlayer.getX(), fromPlayer.getY());

        this.duration = Config.get().getPlayerDestroyedShockwave().getDuration();
        this.durationWithPadding = this.duration + 20;
        this.distance = Config.get().getPlayerDestroyedShockwave().getDistance();
        this.impulse = Config.get().getPlayerDestroyedShockwave().getImpulse();
    }

    static Shockwave shipExploded(Player fromPlayer, Players players, long startedAt) {
        return new Shockwave(fromPlayer, players, startedAt);
    }

    private float range(long elapsed) {
        float ratio = 1.0F - MathUtil.square((elapsed / (float) this.duration) - 1F);
        return this.distance * ratio;
    }

    @Override
    public boolean test(long timestamp) {
        long elapsed = timestamp - this.startedAt;
        if (elapsed > this.durationWithPadding) {
            return false;
        }

        if (this.players.getNumberOfConnectedPlayers() > 0 && elapsed > 0) {
            var dist = range(elapsed);

            players.getSpatialIndex()
                    .search(center.getX(), center.getY(), center.getX(), center.getY(), dist)
                    .forEach(nearPlayer -> handleMove(nearPlayer, dist));

        }

        return true;
    }

    private void handleMove(Player nearPlayer, float dist) {
        if (nearPlayer != fromPlayer && players.isConnected(nearPlayer.id())) {
            Vector2 distanceBetweenPlayers = Vector2
                    .fromXY(nearPlayer.getX(), nearPlayer.getY())
                    .minus(center);

            float absMagnitude = Math.abs(distanceBetweenPlayers.getMagnitude());

            boolean isNotCenteredNorOutOfRange = absMagnitude <= dist && absMagnitude > 0F;
            if (isNotCenteredNorOutOfRange) {

                int sign = distanceBetweenPlayers.getMagnitude() < 0F ? -1 : 1;

                Vector2 move = Vector2
                        .fromAngleMagnitude(
                                distanceBetweenPlayers.getAngle(),
                                sign * this.impulse
                        );

                nearPlayer.move(move.getX(), move.getY(), null);
                GameMetrics.get().getMovedByShockwave().increment(nearPlayer.getClientPlatform());
            }
        }
    }
}
