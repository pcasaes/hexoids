package me.pcasaes.bbop.dto;

public class PlayerLeftEventDto implements EventDto {

    private final String playerId;

    private PlayerLeftEventDto(String playerId) {
        this.playerId = playerId;
    }

    public static PlayerLeftEventDto of (String playerId) {
        return new PlayerLeftEventDto(playerId);
    }

    public String getPlayerId() {
        return playerId;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_LEFT;
    }
}
