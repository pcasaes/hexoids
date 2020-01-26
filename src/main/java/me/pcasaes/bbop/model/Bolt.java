package me.pcasaes.bbop.model;

import java.util.Optional;

import static me.pcasaes.bbop.model.DtoUtils.BOLT_EXHAUSTED_BUILDER;
import static me.pcasaes.bbop.model.DtoUtils.BOLT_MOVED_BUILDER;

public class Bolt {

    private EntityId id;
    private EntityId ownerPlayerId;
    private float prevX;
    private float x;
    private float prevY;
    private float y;
    private float angle;
    private float speed;
    private long previousUpdatesTimestamp;
    private long timestamp;
    private long startTimestamp;
    private boolean exhausted;

    private final Optional<Bolt> optionalThis; // NOSONAR: optional memo

    private final Players players;

    private Bolt(Players players,
                 EntityId boltId,
                 EntityId ownerPlayerId,
                 float x,
                 float y,
                 float angle,
                 float speed,
                 long startTimestamp) {
        this.players = players;
        this.id = boltId;
        this.ownerPlayerId = ownerPlayerId;
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.angle = angle;
        this.speed = speed;
        this.timestamp = startTimestamp;
        this.startTimestamp = this.timestamp;
        this.previousUpdatesTimestamp = this.timestamp;
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
                x,
                y,
                angle,
                Config.get().getBoltSpeed(),
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
            this.previousUpdatesTimestamp = this.timestamp;
            this.timestamp = timestamp;
            return optionalThis;
        }
        return Optional.empty();
    }

    Bolt tackleBoltExhaustion() {
        if (isOutOfBounds() || isExpired()) {
            this.exhausted = true;
            GameEvents.getDomainEvents().register(generateExhaustedEvent());
        }
        return this;
    }

    Bolt move() {

        DomainEvent event = null;
        this.prevX = this.x;
        this.prevY = this.y;
        long elapsed = this.timestamp - this.previousUpdatesTimestamp;
        float r = speed * elapsed / 1000f;

        float ox = this.x;
        float oy = this.y;

        float mx = (float) Math.cos(angle) * r;
        float my = (float) Math.sin(angle) * r;

        float minMove = Config.get().getMinMove();
        if (Math.abs(mx) > minMove) {
            this.x += mx;
        }
        if (Math.abs(my) > minMove) {
            this.y += my;
        }

        if (ox != this.x || oy != this.y) {
            event = generateMovedEvent();
        }

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

    boolean isOutOfBounds() {
        return x < 0f || x > 1f ||
                y < 0f || y > 1f;
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
                player.collision(prevX, prevY, x, y, Config.get().getBoltCollisionRadius());

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
                                                .setX(x)
                                                .setY(y)
                                                .setAngle(angle)
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
