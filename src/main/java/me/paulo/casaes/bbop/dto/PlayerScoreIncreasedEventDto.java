package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class PlayerScoreIncreasedEventDto implements EventDto {
    private final UUID playerId;

    private final int gained;

    private PlayerScoreIncreasedEventDto(UUID playerId, int gained) {
        this.playerId = playerId;
        this.gained = gained;
    }

    @JsonCreator
    public static PlayerScoreIncreasedEventDto increased(
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("gained") int gained) {
        return new PlayerScoreIncreasedEventDto(playerId, gained);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getGained() {
        return gained;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_SCORE_INCREASED;
    }
}
