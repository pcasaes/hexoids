package me.pcasaes.hexoids.core.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import pcasaes.hexoids.proto.Event
import java.util.UUID

class DomainEvent private constructor(
    @get:JsonIgnore val topic: String?,
    val key: UUID,
    val event: Event?
) {

    companion object {

        fun of(
            key: UUID,
            event: Event?
        ): DomainEvent {
            return DomainEvent(null, key, event)
        }

        fun create(topic: String, key: UUID, event: Event): DomainEvent {
            return DomainEvent(topic, key, event)
        }

        fun delete(topic: String, key: UUID): DomainEvent {
            return DomainEvent(topic, key, null)
        }

        fun deleted(key: UUID): DomainEvent {
            return DomainEvent(null, key, null)
        }
    }
}
