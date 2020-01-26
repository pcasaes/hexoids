package me.pcasaes.bbop.model;

import pcasaes.bbop.proto.GUID;

import java.util.Objects;
import java.util.UUID;

public class EntityId {

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
        return of(UUID.fromString(uuid));
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

    private static UUID guidToUuid(GUID guid) {
        return UUID.fromString(guid.getGuid());
    }

    private static GUID uuidToGuid(UUID uuid) {
        return GUID_THREAD_SAFE_BUILDER
                .get()
                .clear()
                .setGuid(uuid.toString())
                .build();
    }

}
