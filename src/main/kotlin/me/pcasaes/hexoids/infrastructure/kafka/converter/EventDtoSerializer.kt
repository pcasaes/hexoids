package me.pcasaes.hexoids.infrastructure.kafka.converter

import org.apache.kafka.common.serialization.Serializer
import pcasaes.hexoids.proto.Event

class EventDtoSerializer : Serializer<Event?> {
    override fun serialize(topic: String, data: Event?): ByteArray? {
        return data?.toByteArray()
    }
}
