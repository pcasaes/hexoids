package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.vector.PositionVector;
import pcasaes.hexoids.proto.BoltExhaustedEventDto;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.Event;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

/**
 * A model representation of a bolt.
 */
public class Bolt {

    private static final Queue<Bolt> POOL = new ArrayDeque<>(1024);

    private EntityId id;
    private EntityId ownerPlayerId;
    private PositionVector positionVector;

    private long timestamp;
    private long startTimestamp;
    private boolean exhausted;
    private int ttl;

    private final Optional<Bolt> optionalThis; // NOSONAR: optional memo

    private final Players players;

    private Bolt(Players players,
                 EntityId boltId,
                 EntityId ownerPlayerId,
                 PositionVector positionVector,
                 long startTimestamp,
                 int ttl) {
        this.players = players;
        this.id = boltId;
        this.ownerPlayerId = ownerPlayerId;
        this.positionVector = positionVector;
        this.timestamp = startTimestamp;
        this.startTimestamp = this.timestamp;
        this.ttl = ttl;
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
     * @param ttl
     * @return
     */
    static Bolt create(Players players,
                       EntityId boltId,
                       EntityId ownerPlayerId,
                       float x,
                       float y,
                       float angle,
                       float speed,
                       long startTimestamp,
                       int ttl) {
        Bolt bolt = POOL.poll();
        if (bolt == null) {
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
                    startTimestamp,
                    ttl);
        } else {
            bolt.id = boltId;
            bolt.ownerPlayerId = ownerPlayerId;
            bolt.startTimestamp = startTimestamp;
            bolt.ttl = ttl;
            bolt.timestamp = startTimestamp;
            bolt.exhausted = false;
            bolt.positionVector.initialized(x, y, angle, speed, startTimestamp);

            return bolt;
        }
    }

    static void destroyObject(Bolt bolt) {
        bolt.id = null;
        bolt.ownerPlayerId = null;
        POOL.offer(bolt);
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

    public void fire(BoltFiredEventDto event) {
        GameEvents.getDomainEvents()
                .dispatch(
                        DomainEvent
                                .create(GameTopic.BOLT_ACTION_TOPIC.name(),
                                        this.id.getId(),
                                        Event.newBuilder()
                                                .setBoltFired(event)
                                                .build()
                                )
                );
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
     * @param timestamp
     * @return
     */
    Bolt tackleBoltExhaustion(long timestamp) {
        if (positionVector.isOutOfBounds() || isExpired()) {
            this.exhausted = true;
            GameEvents.getDomainEvents().dispatch(generateExhaustedEvent(timestamp));
        }
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
        return isExpired(this.timestamp, this.startTimestamp, this.ttl);
    }

    static boolean isExpired(long now, long startTimestamp, int ttl) {
        return now - startTimestamp > ttl;
    }

    /**
     * The inverse of isExhausted.
     *
     * @return
     */
    boolean isActive() {
        return !isExhausted();
    }

    /**
     * Checks if this bolt hit a player
     */
    void checkHits(long timestamp) {
        if (!this.exhausted) {
            this.players
                    .getSpatialIndex()
                    .search(this.positionVector.getPreviousX(), this.positionVector.getPreviousY(),
                            this.positionVector.getX(), this.positionVector.getY(),
                            Config.get().getBoltCollisionIndexSearchDistance())
                    .forEach(p -> hit(p, timestamp));
        }
    }

    private void hit(Player player, long timestamp) {
        boolean isHit = !player.is(ownerPlayerId) &&
                player.collision(positionVector, Config.get().getBoltCollisionRadius());

        if (isHit) {
            player.destroy(this.ownerPlayerId);
            if (!this.exhausted) {
                this.exhausted = true;

                GameEvents.getDomainEvents().dispatch(
                        generateExhaustedEvent(timestamp)
                );
            }
        }
    }

    private DomainEvent generateExhaustedEvent(long timestamp) {
        return DomainEvent
                .create(
                        GameTopic.BOLT_ACTION_TOPIC.name(),
                        this.id.getId(),
                        Event.newBuilder()
                                .setBoltExhausted(
                                        BoltExhaustedEventDto.newBuilder()
                                                .setBoltId(id.getGuid())
                                                .setOwnerPlayerId(ownerPlayerId.getGuid())
                                                .setTimestamp(timestamp)
                                )
                                .build()
                );
    }


    boolean isOwnedBy(EntityId playerId) {
        return this.ownerPlayerId.equals(playerId);
    }


}
