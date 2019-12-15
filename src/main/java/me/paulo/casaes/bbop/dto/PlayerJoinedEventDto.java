package me.paulo.casaes.bbop.dto;

public class PlayerJoinedEventDto extends PlayerDto implements EventDto {


    private PlayerJoinedEventDto(String playerId, int ship, float x, float y, float angle) {
        super(playerId, ship, x, y, angle);
    }

    public static PlayerJoinedEventDto of(String playerId, int ship, float x, float y, float angle) {
        return new PlayerJoinedEventDto(playerId, ship, x, y, angle);
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_JOINED;
    }
}
