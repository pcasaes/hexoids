package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PlayerJoinedEventDto implements EventDto {

    private final String playerId;
    private final int ship;

    private PlayerJoinedEventDto(String playerId, int ship) {
        this.playerId = playerId;
        this.ship = ship;
    }

    @JsonCreator
    public static PlayerJoinedEventDto of(
            @JsonProperty("playerId") String playerId,
            @JsonProperty("ship") int ship) {
        return new PlayerJoinedEventDto(playerId, ship);
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getShip() {
        return ship;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_JOINED;
    }
}
