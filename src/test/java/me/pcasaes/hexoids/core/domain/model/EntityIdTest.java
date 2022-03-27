package me.pcasaes.hexoids.core.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityIdTest {

    @Test
    void testConversion() {
        var uuid = UUID.randomUUID();

        var entityId = EntityId.of(uuid);

        var guid = entityId.getGuid();

        var entityId2 = EntityId.of(guid);

        var uuid2 = entityId2.getId();

        assertEquals(uuid, uuid2);
    }
}