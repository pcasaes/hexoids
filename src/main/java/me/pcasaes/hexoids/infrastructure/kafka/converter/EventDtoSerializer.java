package me.pcasaes.hexoids.infrastructure.kafka.converter;

import org.apache.kafka.common.serialization.Serializer;
import pcasaes.hexoids.proto.Event;

public class EventDtoSerializer implements Serializer<Event> {

    @Override
    public byte[] serialize(String topic, Event data) {
        return data == null ? null : data.toByteArray();
    }
}
