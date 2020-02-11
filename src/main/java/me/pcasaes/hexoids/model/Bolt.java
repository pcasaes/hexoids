package me.pcasaes.hexoids.model;

import me.pcasaes.hexoids.model.vector.PositionVector;

import java.util.Optional;

import static me.pcasaes.hexoids.model.DtoUtils.BOLT_EXHAUSTED_BUILDER;
import static me.pcasaes.hexoids.model.DtoUtils.BOLT_MOVED_BUILDER;

/**
 * A model representation of a bolt.
 */
public class Bolt {

    private final EntityId id;
    private final EntityId ownerPlayerId;
    private final PositionVector positionVector;
    private long timestamp;
    private long startTimestamp;
    private boolean exhausted;

    private final Optional<Bolt> optionalThis; // NOSONAR: optional memo

    private final Players players;

    private Bolt(Players players,
                 EntityId boltId,
                 EntityId ownerPlayerId,
                 PositionVector positionVector,
                 long startTimestamp) {
        this.players = players;
        this.id = boltId;
        this.ownerPlayerId = ownerPlayerId;
        this.positionVector = positionVector;
        this.timestamp = startTimestamp;
        this.startTimestamp = this.timestamp;
        this.exhausted = false;

        this.optionalThis = Optional.of(this);
    }

    /**
     * Creates a bolt
     *
     * @param players
     * @param boltId
     * @param ownerPlayerId
     * @param x
     * @param y
     * @param angle
     * @param speed
     * @param startTimestamp
     * @return
     */
    static Bolt create(Players players,
                       EntityId boltId,
                       EntityId ownerPlayerId,
                       float x,
                       float y,
                       float angle,
                       float speed,
                       long startTimestamp) {
        return new Bolt(
                players,
                boltId,
                ownerPlayerId,
                PositionVector.of(
                        x,
                        y,
                        angle,
                        speed,
                        startTimestamp),
                startTimestamp);
    }

    EntityId getId() {
        return id;
    }

    /**
     * Return true if the id matches this bolt's id.
     *
     * @param id
     * @return
     */
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
            this.positionVector.update(timestamp);
            return optionalThis;
        }
        return Optional.empty();
    }

    /**
     * If bolt is out of bounds or expired will be marked as exhausted
     *
     * @return
     */
    Bolt tackleBoltExhaustion() {
        if (positionVector.isOutOfBounds() || isExpired()) {
            this.exhausted = true;
            GameEvents.getDomainEvents().register(generateExhaustedEvent());
        }
        return this;
    }

    /**
     *
     * Fires a bolt move event.
     *
     * @return
     */
    Bolt move() {

        DomainEvent event = generateMovedEvent();

        GameEvents.getDomainEvents().register(
                event
        );

        return this;
    }

    /**
     * Returns true if exhausted.
     *
     * @return
     */
    boolean isExhausted() {
        return this.exhausted;
    }

    PositionVector getPositionVector() {
        return positionVector;
    }

    private boolean isExpired() {
        return isExpired(this.timestamp, this.startTimestamp);
    }

    static boolean isExpired(long now, long startTimestamp) {
        return now - startTimestamp > Config.get().getBoltMaxDuration();
    }

    /**
     * The inverse of isExhausted.
     * @return
     */
    boolean isActive() {
        return !isExhausted();
    }

    /**
     * Checks if this bolt hit a player
     */
    void checkHits() {
        if (!this.exhausted) {
            this.players.forEach(this::hit);
        }
    }

    private void hit(Player player) {
        boolean isHit = !player.is(ownerPlayerId) &&
                player.collision(positionVector, Config.get().getBoltCollisionRadius());

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
                        GameTopic.BOLT_ACTION_TOPIC.name(),
                        this.id.getId(),
                        DtoUtils
                                .newEvent()
                                .setBoltMoved(
                                        BOLT_MOVED_BUILDER
                                                .clear()
                                                .setBoltId(id.getGuid())
                                                .setOwnerPlayerId(ownerPlayerId.getGuid())
                                                .setX(positionVector.getX())
                                                .setY(positionVector.getY())
                                                .setAngle(positionVector.getVelocity().getAngle())
                                )
                                .build()
                );
    }

    private DomainEvent generateExhaustedEvent() {
        return DomainEvent
                .create(
                        GameTopic.BOLT_ACTION_TOPIC.name(),
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
