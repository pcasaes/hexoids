package me.pcasaes.hexoids.core.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import pcasaes.hexoids.proto.Event
import java.util.UUID

class DomainEvent private constructor(
    @JvmField @get:JsonIgnore val topic: String?,
    @JvmField val key: UUID,
    @JvmField val event: Event?) {

    companion object {
        @JvmStatic
        fun of(
            key: UUID,
            event: Event?
        ): DomainEvent {
            return DomainEvent(null, key, event)
        }

        @JvmStatic
        fun create(topic: String, key: UUID, event: Event): DomainEvent {
            return DomainEvent(topic, key, event)
        }

        @JvmStatic
        fun delete(topic: String, key: UUID): DomainEvent {
            return DomainEvent(topic, key, null)
        }

        @JvmStatic
        fun deleted(key: UUID): DomainEvent {
            return DomainEvent(null, key, null)
        }
    }
}
