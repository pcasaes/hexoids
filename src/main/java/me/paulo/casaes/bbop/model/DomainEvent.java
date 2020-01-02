package me.paulo.casaes.bbop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import me.paulo.casaes.bbop.dto.EventDto;

import java.util.UUID;

public class DomainEvent {
    private final UUID key;
    private final EventDto event;
    private final String topic;

    private DomainEvent(String topic, UUID key, EventDto event) {
        this.topic = topic;
        this.key = key;
        this.event = event;
    }


    public static DomainEvent of(
            UUID key,
            EventDto event) {
        return new DomainEvent(null, key, event);
    }

    public static DomainEvent withoutKey(
            EventDto event) {
        return new DomainEvent(null, null, event);
    }

    public static DomainEvent create(String topic, UUID key, EventDto event) {
        return new DomainEvent(topic, key, event);
    }

    public static DomainEvent delete(String topic, UUID key) {
        return new DomainEvent(topic, key, null);
    }

    public static DomainEvent deleted(UUID key) {
        return new DomainEvent(null, key, null);
    }

    public UUID getKey() {
        return key;
    }

    public EventDto getEvent() {
        return event;
    }

    @JsonIgnore
    public String getTopic() {
        return topic;
    }
}
