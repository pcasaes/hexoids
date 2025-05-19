package me.pcasaes.hexoids.infrastructure.kafka.converter

import org.apache.kafka.common.serialization.Serializer
import java.lang.Long
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class UUIDBytesSerializer : Serializer<UUID> {

    override fun serialize(topic: String?, data: UUID): ByteArray {
        val buffer = ByteBuffer
            .allocate(BUFFER_SIZE)
            .order(ByteOrder.BIG_ENDIAN)
        buffer.putLong(data.mostSignificantBits)
        buffer.putLong(data.leastSignificantBits)
        return buffer.array()
    }

    companion object {
        private val BUFFER_SIZE = Long.BYTES * 2
    }
}
