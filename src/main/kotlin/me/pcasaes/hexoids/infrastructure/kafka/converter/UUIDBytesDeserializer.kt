package me.pcasaes.hexoids.infrastructure.kafka.converter;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class UUIDBytesDeserializer implements Deserializer<UUID> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // do nothing
    }

    @Override
    public UUID deserialize(String topic, byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    @Override
    public void close() {
        //do nothing
    }
}
