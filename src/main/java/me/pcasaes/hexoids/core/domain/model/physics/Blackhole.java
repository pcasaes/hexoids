package me.pcasaes.hexoids.core.domain.model.physics;

import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics;
import me.pcasaes.hexoids.core.domain.model.Bolts;
import me.pcasaes.hexoids.core.domain.model.Clock;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.model.GameEvents;
import me.pcasaes.hexoids.core.domain.model.GameObject;
import me.pcasaes.hexoids.core.domain.model.Players;
import me.pcasaes.hexoids.core.domain.utils.MathUtil;
import me.pcasaes.hexoids.core.domain.vector.Vector2;
import pcasaes.hexoids.proto.ClientPlatforms;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.proto.MassCollapsedIntoBlackHoleEventDto;
import pcasaes.hexoids.proto.MoveReason;

import java.util.Optional;
import java.util.Random;
import java.util.function.LongPredicate;
import java.util.logging.Logger;

public class Blackhole implements LongPredicate {

    private static final Logger LOGGER = Logger.getLogger(Blackhole.class.getName());

    private final EntityId entityId;
    private final String name;
    private final Clock clock;
    private final float eventHorizonRadius;
    private final float gravityRadius;
    private final float gravityImpulse;
    private final float dampenFactor;
    private final Vector2 center;
    private final Players players;
    private final Bolts bolts;
    private final long startTimestamp;
    private final long endTimestamp;
    private final Random rng;
    private final float destroyProbability;

    private Blackhole(EntityId entityId,
                      Vector2 center,
                      long startTimestamp, long endTimestamp,
                      Clock clock,
                      Players players,
                      Bolts bolts) {
        this.entityId = entityId;
        this.center = center;
        this.eventHorizonRadius = Config.get().getBlackhole().getEventHorizonRadius();
        this.gravityRadius = Config.get().getBlackhole().getGravityRadius();
        this.gravityImpulse = Config.get().getBlackhole().getGravityImpulse();
        this.dampenFactor = Config.get().getBlackhole().getDampenFactor();
        this.players = players;
        this.bolts = bolts;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.clock = clock;
        this.rng = new Random();
        this.destroyProbability = 1F - Config.get().getBlackhole().getTeleportProbability();

        String idStr = entityId.toString();
        StringBuilder sbName = new StringBuilder();
        for (char c : idStr.toUpperCase().toCharArray()) {
            boolean firstChar = sbName.isEmpty();
            if (firstChar && c >= 'A' && c <= 'Z') {
                sbName.append((char) (c + 6));
            } else if (!firstChar && c >= '0' && c <= '9') {
                sbName.append(c);
            }
            if (sbName.length() > 3) {
                break;
            }
        }
        sbName.append("*");
        this.name = sbName.toString();
    }

    public static Optional<LongPredicate> massCollapsed(Random rng,
                                                        long startTimestamp, long endTimestamp,
                                                        Clock clock,
                                                        Players players,
                                                        Bolts bolts) {

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

        Blackhole blackhole = new Blackhole(
                entityId,
                Vector2.fromXY(xp, yp),
                startTimestamp, endTimestamp - 10_000L,
                clock,
                players,
                bolts).start();

        GameMetrics.get().getMassCollapsedIntoBlackhole().increment(ClientPlatforms.UNKNOWN);
        LOGGER.info(() -> "Mass collapsed. id = " + blackhole.entityId + ", name = " + blackhole.name + ", center = " + blackhole.center + ",  start = " + blackhole.startTimestamp + ", end = " + blackhole.endTimestamp);

        return Optional.of(blackhole);
    }

    private Blackhole start() {
        Event.Builder eventBuilder = Event.newBuilder();

        final MassCollapsedIntoBlackHoleEventDto massCollapsedIntoBlackHoleEventDto =
                MassCollapsedIntoBlackHoleEventDto.newBuilder()
                        .setId(entityId.getGuid())
                        .setX(center.getX())
                        .setY(center.getY())
                        .setStartTimestamp(startTimestamp)
                        .setEndTimestamp(endTimestamp)
                        .setName(name)
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
    public boolean test(long timestamp) {
        if (this.players.hasConnectedPlayers()) {
            players.getSpatialIndex()
                    .search(center.getX(), center.getY(), center.getX(), center.getY(), gravityRadius)
                    .forEach(p -> {
                        if (players.isConnected(p.id())) {
                            handleMove(p, timestamp);
                        }
                    });
        }

        bolts.forEach(bolt -> handleMove(bolt, timestamp));


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

    private void handleMove(GameObject nearByGameObject, long timestamp) {
        Vector2 distanceFromSingularity = center.minus(Vector2
                .fromXY(nearByGameObject.getX(), nearByGameObject.getY()));

        float absMagnitude = Math.abs(distanceFromSingularity.getMagnitude());

        boolean isNotCenteredNorOutOfRange = absMagnitude < gravityRadius && absMagnitude > 0F;
        if (isNotCenteredNorOutOfRange) {

            int sign = distanceFromSingularity.getMagnitude() < 0F ? -1 : 1;

            boolean hitEventHorizon = absMagnitude <= this.eventHorizonRadius;

            if (hitEventHorizon) {
                boolean destroyed = rng.nextFloat() <= this.destroyProbability;
                if (!destroyed) {
                    float jumpX = rng.nextFloat();
                    float jumpY = rng.nextFloat();
                    destroyed = !nearByGameObject.teleport(jumpX, jumpY, timestamp, MoveReason.BLACKHOLE_TELEPORT);
                }

                if (destroyed) {
                    nearByGameObject.hazardDestroy(entityId, timestamp);
                    GameMetrics.get().getDestroyedByBlackhole().increment(nearByGameObject.getClientPlatform());
                } else {
                    GameMetrics.get().getMovedByBlackhole().increment(nearByGameObject.getClientPlatform());
                }
            } else {

                float acceleration = accel(absMagnitude);

                Vector2 move = Vector2
                        .fromAngleMagnitude(
                                distanceFromSingularity.getAngle(),
                                sign * this.gravityImpulse * acceleration
                        );

                if (nearByGameObject.supportsInertialDampener()) {
                    nearByGameObject
                            .setDampenMovementFactorUntilNextFixedUpdate(1F / (acceleration * this.dampenFactor + 1F));
                }

                nearByGameObject.move(move.getX(), move.getY(), MoveReason.BLACKHOLE_PULL);
                GameMetrics.get().getMovedByBlackhole().increment(nearByGameObject.getClientPlatform());
            }
        }
    }
}
