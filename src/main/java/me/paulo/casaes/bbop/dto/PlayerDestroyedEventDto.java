package me.paulo.casaes.bbop.dto;

public class PlayerDestroyedEventDto implements EventDto {

    private final String playerId;
    private final String destroyedByPlayerId;

    private PlayerDestroyedEventDto(String playerId, String destroyedByPlayerId) {
        this.playerId = playerId;
        this.destroyedByPlayerId = destroyedByPlayerId;
    }

    public static PlayerDestroyedEventDto of(String playerId, String destroyedByPlayerId) {
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
