package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

@RegisterForReflection
public class PlayerScoreUpdatedEventDto implements EventDto {
    private final UUID playerId;

    private final int score;

    private PlayerScoreUpdatedEventDto(UUID playerId, int score) {
        this.playerId = playerId;
        this.score = score;
    }

    @JsonCreator
    public static PlayerScoreUpdatedEventDto updated(
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("score") int score) {
        return new PlayerScoreUpdatedEventDto(playerId, score);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getScore() {
        return score;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_SCORE_UPDATED;
    }
}
