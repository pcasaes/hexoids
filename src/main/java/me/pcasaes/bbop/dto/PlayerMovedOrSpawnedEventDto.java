package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PlayerMovedOrSpawnedEventDto implements EventDto {

    private final String playerId;
    private final float x;
    private final float y;
    private final float angle;
    private final float thrustAngle;
    private final long timestamp;
    private final EventType event;

    private PlayerMovedOrSpawnedEventDto(String playerId,
                                         float x,
                                         float y,
                                         float angle,
                                         float thrustAngle,
                                         long timestamp,
                                         EventType event) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.thrustAngle = thrustAngle;
        this.timestamp = timestamp;
        this.event = event;
    }

    @JsonCreator
    static PlayerMovedOrSpawnedEventDto of(
            @JsonProperty("playerId") String playerId,
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("angle") float angle,
            @JsonProperty("thrustAngle") float thrustAngle,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("event") EventType eventType) {
        return new PlayerMovedOrSpawnedEventDto(playerId, x, y, angle, thrustAngle, timestamp, eventType);
    }

    public static PlayerMovedOrSpawnedEventDto moved(
            String playerId,
            float x,
            float y,
            float angle,
            float thrustAngle,
            long timestamp) {
        return new PlayerMovedOrSpawnedEventDto(playerId, x, y, angle, thrustAngle, timestamp, EventType.PLAYER_MOVED);
    }

    public static PlayerMovedOrSpawnedEventDto spawned(
            String playerId,
            float x,
            float y,
            float angle,
            float thrustAngle,
            long timestamp) {
        return new PlayerMovedOrSpawnedEventDto(playerId, x, y, angle, thrustAngle, timestamp, EventType.PLAYER_SPAWNED);
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

    public float getThrustAngle() {
        return thrustAngle;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public EventType getEvent() {
        return this.event;
    }
}
