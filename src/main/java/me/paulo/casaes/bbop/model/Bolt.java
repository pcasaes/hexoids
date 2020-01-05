package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltExhaustedEventDto;
import me.paulo.casaes.bbop.dto.BoltMovedEventDto;

import java.util.Optional;
import java.util.UUID;

public class Bolt {

    private UUID id;
    private String idString;
    private UUID ownerPlayerId;
    private String ownerPlayerIdStr;
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

    private final Optional<Bolt> optionalThis;

    private Bolt(UUID boltId, UUID ownerPlayerId, float x, float y, float angle, float speed, long startTimestamp) {
        this.id = boltId;
        this.idString = this.id.toString();
        this.ownerPlayerId = ownerPlayerId;
        this.ownerPlayerIdStr = ownerPlayerId.toString();
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

    static Bolt create(UUID boltId,
                       UUID ownerPlayerId,
                       float x,
                       float y,
                       float angle,
                       long startTimestamp) {
        return new Bolt(
                boltId,
                ownerPlayerId,
                x,
                y,
                angle,
                Config.get().getBoltSpeed(),
                startTimestamp);
    }

    UUID getId() {
        return id;
    }

    boolean is(UUID id) {
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
        return this.timestamp - this.startTimestamp > Config.get().getBoltMaxDuration();
    }

    boolean isActive() {
        return !isExhausted();
    }

    void checkHits() {
        if (!this.exhausted) {
            Players.get()
                    .forEach(this::hit);
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
                        Topics.BoltActionTopic.name(),
                        this.id,
                        BoltMovedEventDto.of(this.idString, this.ownerPlayerIdStr, this.x, this.y, this.angle)
                );
    }

    private DomainEvent generateExhaustedEvent() {
        return DomainEvent
                .create(
                        Topics.BoltActionTopic.name(),
                        this.id,
                        BoltExhaustedEventDto.of(this.idString, this.ownerPlayerIdStr)
                );
    }


    boolean isOwnedBy(UUID playerId) {
        return this.ownerPlayerId.equals(playerId);
    }


}
