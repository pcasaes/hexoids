package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.BoltExhaustedEventDto;
import me.paulo.casaes.bbop.dto.BoltMovedEventDto;
import me.paulo.casaes.bbop.dto.EventDto;

import java.util.UUID;

public class Bolt {

    private UUID id;
    private String idString;
    private String ownerPlayerId;
    private float prevX;
    private float x;
    private float prevY;
    private float y;
    private float angle;
    private float speed;
    private long timestamp;
    private long startTimestamp;
    private boolean exhausted;

    private Bolt(UUID boltId, String ownerPlayerId, float x, float y, float angle, float speed, long startTimestamp) {
        this.id = boltId;
        this.idString = this.id.toString();
        this.ownerPlayerId = ownerPlayerId;
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.angle = angle;
        this.speed = speed;
        this.timestamp = startTimestamp;
        this.startTimestamp = this.timestamp;
        this.exhausted = false;
    }

    static Bolt create(UUID boltId,
                       String ownerPlayerId,
                       float x,
                       float y,
                       float angle,
                       float speedAdjustment,
                       long startTimestamp) {
        return new Bolt(
                boltId,
                ownerPlayerId,
                x,
                y,
                angle,
                Config.get().getBoltSpeed() + speedAdjustment,
                startTimestamp);
    }

    UUID getId() {
        return id;
    }

    boolean is(UUID id) {
        return this.id.equals(id);
    }

    void move(long timestamp) {
        long elapsed = Math.max(0L, timestamp - this.timestamp);
        this.timestamp = timestamp;

        EventDto event = null;
        this.prevX = this.x;
        this.prevY = this.y;
        if (elapsed > 0L) {
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
                event = toEvent();
            }
        }

        if (event != null) {
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(
                            Topics.BoltActionTopic.name(),
                            this.idString,
                            event
                    )
            );
        }
    }

    boolean isExhausted() {
        if (this.exhausted) {
            return true;
        }
        if (x < 0f || x > 1f ||
                y < 0f || y > 1f ||
                this.timestamp - this.startTimestamp > Config.get().getBoltMaxDuration()) {
            this.exhausted = true;
            return true;
        }
        return false;
    }

    boolean isActive() {
        return !isExhausted();
    }

    void checkHits() {
        Players.get()
                .forEach(this::hit);
    }

    private void hit(Player player) {
        boolean isHit = !player.is(ownerPlayerId) && player.collision(prevX, prevY, x, y, Config.get().getBoltCollisionRadius());

        if (isHit) {
            player.destroyedBy(this.ownerPlayerId);
            if (!this.exhausted) {
                this.exhausted = true;

                GameEvents.getDomainEvents().register(
                        DomainEvent.create(
                                Topics.BoltActionTopic.name(),
                                this.idString,
                                BoltExhaustedEventDto.of(this.idString, this.ownerPlayerId)
                        )
                );
            }
        }
    }

    EventDto toEvent() {
        return isExhausted() ?
                BoltExhaustedEventDto.of(this.idString, this.ownerPlayerId) :
                BoltMovedEventDto.of(this.idString, this.ownerPlayerId, this.x, this.y, this.angle);
    }


    boolean isOwnedBy(String playerId) {
        return this.ownerPlayerId.equals(playerId);
    }


}
