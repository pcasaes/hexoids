package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerJoinedEventDto extends PlayerDto implements EventDto {


    private PlayerJoinedEventDto(String playerId, int ship, float x, float y, float angle) {
        super(playerId, ship, x, y, angle);
    }

    @JsonCreator
    public static PlayerJoinedEventDto of(
            @JsonProperty("playerId") String playerId,
            @JsonProperty("ship") int ship,
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("angle") float angle) {
        return new PlayerJoinedEventDto(playerId, ship, x, y, angle);
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_JOINED;
    }
}
