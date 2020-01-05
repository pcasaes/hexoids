package me.paulo.casaes.bbop.dto;

public enum EventType {
    PLAYER_JOINED(PlayerJoinedEventDto.class),
    PLAYER_MOVED(PlayerMovedEventDto.class),
    PLAYER_LEFT(PlayerLeftEventDto.class),
    PLAYER_DESTROYED(PlayerDestroyedEventDto.class),
    PLAYER_SCORE_UPDATED(PlayerScoreUpdatedEventDto.class),
    PLAYER_SCORE_INCREASED(PlayerScoreIncreasedEventDto.class),

    BOLT_FIRED(BoltFiredEventDto.class),
    BOLT_MOVED(BoltMovedEventDto.class),
    BOLT_EXHAUSTED(BoltExhaustedEventDto.class),

    SCOREBOARD_UPDATED(ScoreBoardUpdatedEventDto.class),
    ;

    private final Class<? extends EventDto> classType;

    EventType(Class<? extends EventDto> classType) {
        this.classType = classType;
    }

    public Class<? extends EventDto> getClassType() {
        return classType;
    }
}
