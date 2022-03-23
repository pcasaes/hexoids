package me.pcasaes.hexoids.core.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import pcasaes.hexoids.proto.Event;

import java.util.UUID;

public class DomainEvent {
    private final UUID key;
    private final Event event;
    private final String topic;

    private DomainEvent(String topic, UUID key, Event event) {
        this.topic = topic;
        this.key = key;
        this.event = event;
    }


    public static DomainEvent of(
            UUID key,
            Event event) {
        return new DomainEvent(null, key, event);
    }

    public static DomainEvent create(String topic, UUID key, Event event) {
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

    public Event getEvent() {
        return event;
    }

    @JsonIgnore
    public String getTopic() {
        return topic;
    }
}
