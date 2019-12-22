package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerMovedEventDto implements EventDto {

    private final String playerId;
    private final float x;
    private final float y;
    private final float angle;
    private final float currentSpeed;
    private final long timestamp;

    private PlayerMovedEventDto(String playerId, float x, float y, float angle, float currentSpeed, long timestamp) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.currentSpeed = currentSpeed;
        this.timestamp = timestamp;
    }

    @JsonCreator
    public static PlayerMovedEventDto of(
            @JsonProperty("playerId") String playerId,
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("angle") float angle,
            @JsonProperty("currentSpeed") float currentSpeed,
            @JsonProperty("timestamp") long timestamp) {
        return new PlayerMovedEventDto(playerId, x, y, angle, currentSpeed, timestamp);
    }

    public String getPlayerId() {
        return playerId;
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

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_MOVED;
    }
}
