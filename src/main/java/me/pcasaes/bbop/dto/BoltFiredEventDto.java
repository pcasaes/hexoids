package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class BoltFiredEventDto implements EventDto {

    private final UUID boltId;
    private final UUID ownerPlayerId;
    private final float x;
    private final float y;
    private final float angle;
    private final long startTimestamp;

    private BoltFiredEventDto(UUID boltId,
                              UUID ownerPlayerId,
                              float x,
                              float y,
                              float angle,
                              long startTimestamp) {
        this.boltId = boltId;
        this.ownerPlayerId = ownerPlayerId;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.startTimestamp = startTimestamp;
    }

    @JsonCreator
    public static BoltFiredEventDto of(
            @JsonProperty("boltId") UUID boltId,
            @JsonProperty("ownerPlayerId") UUID ownerPlayerId,
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("angle") float angle,
            @JsonProperty("startTimestamp") long startTimestamp) {
        return new BoltFiredEventDto(boltId,
                ownerPlayerId,
                x,
                y,
                angle,
                startTimestamp);
    }

    public UUID getBoltId() {
        return boltId;
    }

    public UUID getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getAngle() {
        return angle;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    @Override
    public EventType getEvent() {
        return EventType.BOLT_FIRED;
    }
}
