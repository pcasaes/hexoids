package me.paulo.casaes.bbop.service.kafka.converter;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class UUIDBytesSerializer implements Serializer<UUID> {

    private static final int BUFFER_SIZE = Long.BYTES * 2;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        //do nothing
    }

    @Override
    public byte[] serialize(String topic, UUID data) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.putLong(data.getMostSignificantBits());
        buffer.putLong(data.getLeastSignificantBits());
        return buffer.array();
    }

    @Override
    public void close() {
        //do nothing
    }
}
