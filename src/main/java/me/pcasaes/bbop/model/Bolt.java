package me.pcasaes.bbop.model;

import java.util.Optional;

import static me.pcasaes.bbop.model.DtoUtils.BOLT_EXHAUSTED_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.BOLT_MOVED_BUILDER;

public class Bolt {

    private final EntityId id;
    private final EntityId ownerPlayerId;
    private final VelocityVector velocityVector;
    private long timestamp;
    private long startTimestamp;
    private boolean exhausted;

    private final Optional<Bolt> optionalThis; // NOSONAR: optional memo

    private final Players players;

    private Bolt(Players players,
                 EntityId boltId,
                 EntityId ownerPlayerId,
                 VelocityVector velocityVector,
                 long startTimestamp) {
        this.players = players;
        this.id = boltId;
        this.ownerPlayerId = ownerPlayerId;
        this.velocityVector = velocityVector;
        this.timestamp = startTimestamp;
        this.startTimestamp = this.timestamp;
        this.exhausted = false;

        this.optionalThis = Optional.of(this);
    }

    static Bolt create(Players players,
                       EntityId boltId,
                       EntityId ownerPlayerId,
                       float x,
                       float y,
                       float angle,
                       long startTimestamp) {
        return new Bolt(
                players,
                boltId,
                ownerPlayerId,
                VelocityVector.of(
                        x,
                        y,
                        angle,
                        Config.get().getBoltSpeed(),
                        startTimestamp),
                startTimestamp);
    }

    EntityId getId() {
        return id;
    }

    boolean is(EntityId id) {
        return this.id.equals(id);
    }

    /**
     * Updates the this bolt's internal timestamp.
     *
     * @param timestamp
     * @return return empty if the timestamp hasn't changed
     */
    Optional<Bolt> updateTimestamp(long timestamp) {
        long elapsed = Math.max(0L, timestamp - this.timestamp);
        if (elapsed > 0L) {
            this.timestamp = timestamp;
            this.velocityVector.update(timestamp);
            return optionalThis;
        }
        return Optional.empty();
    }

    Bolt tackleBoltExhaustion() {
        if (velocityVector.isOutOfBounds() || isExpired()) {
            this.exhausted = true;
            GameEvents.getDomainEvents().register(generateExhaustedEvent());
        }
        return this;
    }

    Bolt move() {

        DomainEvent event = generateMovedEvent();

        if (event != null) {
            GameEvents.getDomainEvents().register(
                    event
            );
        }

        return this;
    }

    boolean isExhausted() {
        return this.exhausted;
    }

    VelocityVector getVelocityVector() {
        return velocityVector;
    }

    boolean isExpired() {
        return isExpired(this.timestamp, this.startTimestamp);
    }

    static boolean isExpired(long now, long startTimestamp) {
        return now - startTimestamp > Config.get().getBoltMaxDuration();
    }


    boolean isActive() {
        return !isExhausted();
    }

    void checkHits() {
        if (!this.exhausted) {
            this.players.forEach(this::hit);
        }
    }

    private void hit(Player player) {
        boolean isHit = !player.is(ownerPlayerId) &&
                player.collision(velocityVector, Config.get().getBoltCollisionRadius());

        if (isHit) {
            player.destroy(this.ownerPlayerId);
            if (!this.exhausted) {
                this.exhausted = true;

                GameEvents.getDomainEvents().register(
                        generateExhaustedEvent()
                );
            }
        }
    }

    DomainEvent generateMovedEvent() {
        return DomainEvent
                .create(
                        Topics.BOLT_ACTION_TOPIC.name(),
                        this.id.getId(),
                        DtoUtils
                                .newEvent()
                                .setBoltMoved(
                                        BOLT_MOVED_BUILDER
                                                .clear()
                                                .setBoltId(id.getGuid())
                                                .setOwnerPlayerId(ownerPlayerId.getGuid())
                                                .setX(velocityVector.getX())
                                                .setY(velocityVector.getY())
                                                .setAngle(velocityVector.getAngle())
                                )
                                .build()
                );
    }

    private DomainEvent generateExhaustedEvent() {
        return DomainEvent
                .create(
                        Topics.BOLT_ACTION_TOPIC.name(),
                        this.id.getId(),
                        DtoUtils
                                .newEvent()
                                .setBoltExhausted(
                                        BOLT_EXHAUSTED_BUILDER
                                                .clear()
                                                .setBoltId(id.getGuid())
                                                .setOwnerPlayerId(ownerPlayerId.getGuid())
                                )
                                .build()
                );
    }


    boolean isOwnedBy(EntityId playerId) {
        return this.ownerPlayerId.equals(playerId);
    }


}
