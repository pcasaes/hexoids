package me.pcasaes.hexoids.core.domain.model

import pcasaes.hexoids.proto.Event
import java.util.UUID

data class EventRecord(
    val key: UUID,
    val event: Event?
) {
    companion object {
        fun of(domainEvent: DomainEvent): EventRecord {
            return EventRecord(domainEvent.key, domainEvent.event)
        }

        fun of(key: UUID, event: Event?): EventRecord {
            return EventRecord(key, event)
        }
    }
}