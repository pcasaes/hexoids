package me.pcasaes.hexoids.infrastructure.kafka.converter

import org.apache.kafka.common.serialization.Deserializer
import java.nio.ByteBuffer
import java.util.UUID

class UUIDBytesDeserializer : Deserializer<UUID> {

    override fun deserialize(topic: String, data: ByteArray): UUID {
        val bb = ByteBuffer.wrap(data)
        val high = bb.getLong()
        val low = bb.getLong()
        return UUID(high, low)
    }
}
