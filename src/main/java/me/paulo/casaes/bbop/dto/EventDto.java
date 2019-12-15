package me.paulo.casaes.bbop.dto;

public interface EventDto extends Dto {

    EventType getEvent();

    default boolean isEvent(EventType eventType) {
        return eventType == getEvent();
    }
}
