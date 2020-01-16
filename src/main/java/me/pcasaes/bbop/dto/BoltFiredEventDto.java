package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BoltFiredEventDto implements EventDto {

    private final String boltId;
    private final String ownerPlayerId;
    private final float x;
    private final float y;
    private final float angle;
    private final long startTimestamp;

    private BoltFiredEventDto(String boltId,
                              String ownerPlayerId,
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
            @JsonProperty("boltId") String boltId,
            @JsonProperty("ownerPlayerId") String ownerPlayerId,
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

    public String getBoltId() {
        return boltId;
    }

    public String getOwnerPlayerId() {
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
