package me.pcasaes.hexoids.infrastructure.kafka.converter

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.Long
import java.util.UUID

class UUIDBytesSerializerDeserializerTest {
    @Test
    fun tessUUIDSerializerAndDeserializer() {
        val one = UUID.randomUUID()

        val oneBytes = UUIDBytesSerializer().serialize("", one)
        Assertions.assertNotNull(oneBytes)
        Assertions.assertEquals(Long.BYTES * 2, oneBytes.size)

        val oneDeserialized = UUIDBytesDeserializer().deserialize("", oneBytes)
        Assertions.assertNotNull(oneDeserialized)

        Assertions.assertEquals(one, oneDeserialized)
        Assertions.assertNotSame(one, oneDeserialized)
    }
}