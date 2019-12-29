package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BoltFiredEventDto implements EventDto {

    private final String boltId;
    private final String ownerPlayerId;
    private final float x;
    private final float y;
    private final float angle;
    private final float speedAdjustment;
    private final long startTimestamp;

    private BoltFiredEventDto(String boltId,
                              String ownerPlayerId,
                              float x,
                              float y,
                              float angle,
                              float speedAdjustment,
                              long startTimestamp) {
        this.boltId = boltId;
        this.ownerPlayerId = ownerPlayerId;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speedAdjustment = speedAdjustment;
        this.startTimestamp = startTimestamp;
    }

    @JsonCreator
    public static BoltFiredEventDto of(
            @JsonProperty("boltId") String boltId,
            @JsonProperty("ownerPlayerId") String ownerPlayerId,
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("angle") float angle,
            @JsonProperty("speedAdjustment") float speedAdjustment,
            @JsonProperty("startTimestamp") long startTimestamp) {
        return new BoltFiredEventDto(boltId,
                ownerPlayerId,
                x,
                y,
                angle,
                speedAdjustment,
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

    public float getSpeedAdjustment() {
        return speedAdjustment;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    @Override
    public EventType getEvent() {
        return EventType.BOLT_FIRED;
    }
}
