package me.paulo.casaes.bbop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import me.paulo.casaes.bbop.dto.EventDto;

public class DomainEvent {
    private final String key;
    private final EventDto event;
    private final String topic;

    private DomainEvent(String topic, String key, EventDto event) {
        this.topic = topic;
        this.key = key;
        this.event = event;
    }


    public static DomainEvent of(
            String key,
            EventDto event) {
        return new DomainEvent(null, key, event);
    }

    public static DomainEvent create(String topic, String key, EventDto event) {
        return new DomainEvent(topic, key, event);
    }

    public static DomainEvent delete(String topic, String key) {
        return new DomainEvent(topic, key, null);
    }

    public static DomainEvent deleted(String key) {
        return new DomainEvent(null, key, null);
    }

    public String getKey() {
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
