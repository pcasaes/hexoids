package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

@RegisterForReflection
public class PlayerDestroyedEventDto implements EventDto {

    private final UUID playerId;
    private final UUID destroyedByPlayerId;

    private PlayerDestroyedEventDto(UUID playerId, UUID destroyedByPlayerId) {
        this.playerId = playerId;
        this.destroyedByPlayerId = destroyedByPlayerId;
    }

    @JsonCreator
    public static PlayerDestroyedEventDto of(
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("destroyedByPlayerId") UUID destroyedByPlayerId) {
        return new PlayerDestroyedEventDto(playerId, destroyedByPlayerId);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getDestroyedByPlayerId() {
        return destroyedByPlayerId;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_DESTROYED;
    }
}
