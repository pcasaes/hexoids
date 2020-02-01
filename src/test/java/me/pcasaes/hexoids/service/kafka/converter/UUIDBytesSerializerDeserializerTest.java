package me.pcasaes.hexoids.service.kafka.converter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class UUIDBytesSerializerDeserializerTest {

    @Test
    void tessUUIDSerializerAndDeserializer() {
        UUID one = UUID.randomUUID();

        byte[] oneBytes = new UUIDBytesSerializer().serialize("", one);
        assertNotNull(oneBytes);
        assertEquals(Long.BYTES * 2, oneBytes.length);

        UUID oneDeserialized = new UUIDBytesDeserializer().deserialize("", oneBytes);
        assertNotNull(oneDeserialized);

        assertEquals(one, oneDeserialized);
        assertNotSame(one, oneDeserialized);
    }
}