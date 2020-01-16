package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface EventDto extends Dto {

    EventType getEvent();

    default boolean isEvent(EventType eventType) {
        return eventType == getEvent();
    }

    @Override
    @JsonIgnore
    default Dto.Type getDtoType() {
        return DtoType.EVENT_DTO;
    }

    enum DtoType implements Dto.Type {
        EVENT_DTO;
    }

}
