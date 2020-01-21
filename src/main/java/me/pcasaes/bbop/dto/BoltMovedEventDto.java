package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class BoltMovedEventDto implements EventDto {

    private final String boltId;
    private final String ownerPlayerId;
    private final float x;
    private final float y;
    private final float angle;

    private BoltMovedEventDto(String boltId, String ownerPlayerId, float x, float y, float angle) {
        this.boltId = boltId;
        this.ownerPlayerId = ownerPlayerId;
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    @JsonCreator
    public static BoltMovedEventDto of(
            @JsonProperty("boltId") String boltId,
            @JsonProperty("ownerPlayerId") String ownerPlayerId,
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("angle") float angle) {
        return new BoltMovedEventDto(boltId, ownerPlayerId, x, y, angle);
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

    @Override
    public EventType getEvent() {
        return EventType.BOLT_MOVED;
    }
}
