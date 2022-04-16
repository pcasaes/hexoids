package me.pcasaes.hexoids.core.domain.model.physics;

import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics;
import me.pcasaes.hexoids.core.domain.model.Clock;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.GameEvents;
import me.pcasaes.hexoids.core.domain.model.Player;
import me.pcasaes.hexoids.core.domain.model.Players;
import me.pcasaes.hexoids.core.domain.utils.MathUtil;
import me.pcasaes.hexoids.core.domain.vector.Vector2;
import pcasaes.hexoids.proto.ClientPlatforms;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.proto.MassCollapsedIntoBlackHoleEventDto;

import java.util.Optional;
import java.util.Random;
import java.util.function.LongPredicate;
import java.util.logging.Logger;

public class Blackhole implements LongPredicate {

    private static final Logger LOGGER = Logger.getLogger(Blackhole.class.getName());

    private final EntityId entityId;
    private final Clock clock;
    private final float eventHorizonRadius;
    private final float gravityRadius;
    private final float gravityImpulse;
    private final float dampenFactor;
    private final Vector2 center;
    private final Players players;
    private final long startTimestamp;
    private final long endTimestamp;

    private Blackhole(EntityId entityId, float x, float y, long startTimestamp, long endTimestamp, Clock clock, Players players) {
        this.entityId = entityId;
        this.center = Vector2.fromXY(x, y);
        this.eventHorizonRadius = Config.get().getBlackhole().getEventHorizonRadius();
        this.gravityRadius = Config.get().getBlackhole().getGravityRadius();
        this.gravityImpulse = Config.get().getBlackhole().getGravityImpulse();
        this.dampenFactor = Config.get().getBlackhole().getDampenFactor();
        this.players = players;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.clock = clock;
    }

    public static Optional<LongPredicate> massCollapsed(Random rng, long startTimestamp, long endTimestamp, Clock clock, Players players) {

        if (rng.nextInt(Config.get().getBlackhole().getGenesisProbabilityFactor()) > 0) {
            return Optional.empty();
        }

        EntityId entityId = EntityId.newId(rng);
        float xp = rng.nextFloat();
        float yp = rng.nextFloat();

        // we must do this check AFTER consuming the RNG otherwise we introduce non-deterministic behavior
        if (endTimestamp < clock.getTime()) {
            return Optional.empty();
        }

        Blackhole blackhole = new Blackhole(entityId, xp, yp, startTimestamp, endTimestamp - 10_000L, clock, players).start();

        GameMetrics.get().getMassCollapsedIntoBlackhole().increment(ClientPlatforms.UNKNOWN);
        LOGGER.info(() -> "Mass collapsed. id = " + blackhole.entityId + ", center = " + blackhole.center + ",  start = " + blackhole.startTimestamp + ", end = " + blackhole.endTimestamp);

        return Optional.of(blackhole);
    }

    private Blackhole start() {
        Event.Builder eventBuilder = Event.newBuilder();

        final MassCollapsedIntoBlackHoleEventDto massCollapsedIntoBlackHoleEventDto = MassCollapsedIntoBlackHoleEventDto.newBuilder()
                .setX(center.getX())
                .setY(center.getY())
                .setStartTimestamp(startTimestamp)
                .setEndTimestamp(endTimestamp)
                .build();

        eventBuilder.setMassCollapsedIntoBlackHole(massCollapsedIntoBlackHoleEventDto);

        players.registerCurrentViewModifier(entityId, currentViewBuilder -> {
            if (clock.getTime() < endTimestamp) {
                currentViewBuilder.setBlackhole(massCollapsedIntoBlackHoleEventDto);
            }
        });

        GameEvents
                .getClientEvents()
                .dispatch(
                        Dto.newBuilder()
                                .setEvent(eventBuilder)
                                .build()
                );
        return this;
    }

    @Override
    public boolean test(long l) {
        if (this.players.hasConnectedPlayers()) {
            players.getSpatialIndex()
                    .search(center.getX(), center.getY(), center.getX(), center.getY(), gravityRadius)
                    .forEach(this::handleMove);
        }


        boolean exists = clock.getTime() < endTimestamp;

        if (!exists) {
            GameMetrics.get().getBlackholeEvaporated().increment(ClientPlatforms.UNKNOWN);
            LOGGER.info(() -> "Blackhole evaporated: " + entityId);
            players.unregisterCurrentViewModifier(entityId);
        }
        return exists;
    }

    private float accel(float absMagnitude) {
        float relDistance = absMagnitude / this.gravityRadius;
        float invDistance = 1.0F - relDistance;
        return MathUtil.cube(invDistance);
    }

    private void handleMove(Player nearPlayer) {
        if (players.isConnected(nearPlayer.id())) {
            Vector2 distanceBetweenPlayers = center.minus(Vector2
                    .fromXY(nearPlayer.getX(), nearPlayer.getY()));

            float absMagnitude = Math.abs(distanceBetweenPlayers.getMagnitude());

            boolean isNotCenteredNorOutOfRange = absMagnitude < gravityRadius && absMagnitude > 0F;
            if (isNotCenteredNorOutOfRange) {

                int sign = distanceBetweenPlayers.getMagnitude() < 0F ? -1 : 1;

                boolean destroyed = absMagnitude <= this.eventHorizonRadius;

                if (destroyed) {
                    nearPlayer.hazardDestroy(entityId);
                    GameMetrics.get().getDestroyedByBlackhole().increment(nearPlayer.getClientPlatform());
                } else {

                    float acceleration = accel(absMagnitude);

                    Vector2 move = Vector2
                            .fromAngleMagnitude(
                                    distanceBetweenPlayers.getAngle(),
                                    sign * this.gravityImpulse * acceleration
                            );

                    nearPlayer.setDampenMovementFactorUntilNextFixedUpdate(1F / (acceleration * this.dampenFactor + 1F));

                    nearPlayer.move(move.getX(), move.getY(), null);
                    GameMetrics.get().getMovedByBlackhole().increment(nearPlayer.getClientPlatform());
                }
            }
        }
    }
}
