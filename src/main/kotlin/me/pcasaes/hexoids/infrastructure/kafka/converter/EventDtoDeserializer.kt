package me.pcasaes.hexoids.infrastructure.kafka.converter

import com.google.protobuf.InvalidProtocolBufferException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import pcasaes.hexoids.proto.Event
import java.util.logging.Level
import java.util.logging.Logger

class EventDtoDeserializer : Deserializer<Event?> {
    private val stringDeserializer = StringDeserializer()


    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {
        stringDeserializer.configure(configs, isKey)
    }

    override fun deserialize(topic: String, data: ByteArray?): Event? {
        if (data == null) {
            return null
        }
        try {
            return Event.newBuilder()
                .mergeFrom(data)
                .build()
        } catch (ex: InvalidProtocolBufferException) {
            LOGGER.log(Level.SEVERE, ex.message, ex)
        }
        return null
    }

    override fun close() {
        stringDeserializer.close()
        super.close()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(EventDtoDeserializer::class.java.getName())
    }
}
