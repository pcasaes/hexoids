package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BoltExhaustedEventDto implements EventDto {

    private final String boltId;
    private final String ownerPlayerId;

    private BoltExhaustedEventDto(String boltId, String ownerPlayerId) {
        this.boltId = boltId;
        this.ownerPlayerId = ownerPlayerId;
    }

    @JsonCreator
    public static BoltExhaustedEventDto of(
            @JsonProperty("boltId") String boltId,
            @JsonProperty("ownerPlayerId") String ownerPlayerId) {
        return new BoltExhaustedEventDto(boltId, ownerPlayerId);
    }

    public String getBoltId() {
        return boltId;
    }

    public String getOwnerPlayerId() {
        return ownerPlayerId;
    }

    @Override
    public EventType getEvent() {
        return EventType.BOLT_EXHAUSTED;
    }
}
