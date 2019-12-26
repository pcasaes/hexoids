package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerDestroyedEventDto implements EventDto {

    private final String playerId;
    private final String destroyedByPlayerId;

    private PlayerDestroyedEventDto(String playerId, String destroyedByPlayerId) {
        this.playerId = playerId;
        this.destroyedByPlayerId = destroyedByPlayerId;
    }

    @JsonCreator
    public static PlayerDestroyedEventDto of(
            @JsonProperty("playerId") String playerId,
            @JsonProperty("destroyedByPlayerId") String destroyedByPlayerId) {
        return new PlayerDestroyedEventDto(playerId, destroyedByPlayerId);
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getDestroyedByPlayerId() {
        return destroyedByPlayerId;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_DESTROYED;
    }
}
