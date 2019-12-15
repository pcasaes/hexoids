package me.paulo.casaes.bbop.dto;

public class PlayerMovedEventDto implements EventDto {

    private final String playerId;
    private final float x;
    private final float y;
    private final float angle;

    private PlayerMovedEventDto(String playerId, float x, float y, float angle) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public static PlayerMovedEventDto of(String playerId, float x, float y, float angle) {
        return new PlayerMovedEventDto(playerId, x, y, angle);
    }

    public String getPlayerId() {
        return playerId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getAngle() {
        return angle;
    }

    @Override
    public EventType getEvent() {
        return EventType.PLAYER_MOVED;
    }
}
