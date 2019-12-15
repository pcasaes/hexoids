package me.paulo.casaes.bbop.dto;

public class BoltExhaustedEventDto implements EventDto {

    private final String boltId;

    private BoltExhaustedEventDto(String boltId) {
        this.boltId = boltId;
    }

    public static BoltExhaustedEventDto of(String boltId) {
        return new BoltExhaustedEventDto(boltId);
    }

    public String getBoltId() {
        return boltId;
    }

    @Override
    public EventType getEvent() {
        return EventType.BOLT_EXHAUSTED;
    }
}
