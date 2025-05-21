package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.of
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

class EntityIdTest {
    @Test
    fun testConversion() {
        val uuid = UUID.randomUUID()

        val entityId = of(uuid)

        val guid = entityId.getGuid()

        val entityId2 = of(guid)

        val uuid2 = entityId2.getId()

        Assertions.assertEquals(uuid, uuid2)
    }
}