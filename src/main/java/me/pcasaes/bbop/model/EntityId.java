package me.pcasaes.bbop.model;

import pcasaes.bbop.proto.GUID;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class EntityId {

    private static final Logger LOGGER = Logger.getLogger(EntityId.class.getName());

    private static final ThreadLocal<GUID.Builder> GUID_THREAD_SAFE_BUILDER = ThreadLocal.withInitial(GUID::newBuilder);

    private final UUID id;
    private final GUID guid;

    private EntityId(UUID id, GUID guid) {
        this.id = id;
        this.guid = guid;
    }

    public static EntityId of(UUID id) {
        return new EntityId(id, uuidToGuid(id));
    }

    public static EntityId of(GUID guid) {
        return new EntityId(guidToUuid(guid), guid);
    }

    public static EntityId of(String uuid) {
        return of(stringToUuid(uuid));
    }

    public static EntityId newId() {
        return of(UUID.randomUUID());
    }


    public UUID getId() {
        return id;
    }

    public GUID getGuid() {
        return guid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityId entityId = (EntityId) o;
        return Objects.equals(id, entityId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    private static UUID stringToUuid(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException ex) {
            LOGGER.severe(() -> "Could not deserialize uuid: '" + uuid + "'");
            throw ex;
        }
    }

    private static UUID guidToUuid(GUID guid) {
        return stringToUuid(guid.getGuid());
    }

    private static GUID uuidToGuid(UUID uuid) {
        return GUID_THREAD_SAFE_BUILDER
                .get()
                .clear()
                .setGuid(uuid.toString())
                .build();
    }

}
