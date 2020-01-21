package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

@RegisterForReflection
public class PlayerScoreIncreasedEventDto implements EventDto {
    private final UUID playerId;

    private final int gained;

    private final long timestamp;

    private PlayerScoreIncreasedEventDto(UUID playerId, int gained, long timestamp) {
        this.playerId = playerId;
        this.gained = gained;
        this.timestamp = timestamp;
    }

    @JsonCreator
    public static PlayerScoreIncreasedEventDto increased(
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("gained") int gained,
            @JsonProperty("timestamp") long timestamp) {
        return new PlayerScoreIncreasedEventDto(playerId, gained, timestamp);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getGained() {
        return gained;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_SCORE_INCREASED;
    }
}
