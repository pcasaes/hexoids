package me.pcasaes.hexoids.model;

import pcasaes.hexoids.proto.GUID;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Identifier immutable.
 *
 * The identifier uses a {@link UUID} and holds its DTO {@link GUID} analog.
 *
 */
public class EntityId {

    private static final Logger LOGGER = Logger.getLogger(EntityId.class.getName());

    private static final ThreadLocal<GUID.Builder> GUID_THREAD_SAFE_BUILDER = ThreadLocal.withInitial(GUID::newBuilder);

    private final UUID id;
    private final GUID guid;

    private EntityId(UUID id, GUID guid) {
        this.id = id;
        this.guid = guid;
    }

    /**
     * Constructs an EntityId from a {@link UUID}
     * @param id a uuid
     * @return
     */
    public static EntityId of(UUID id) {
        return new EntityId(id, uuidToGuid(id));
    }

    /**
     * Constructs an EntityId from a DTO {@link GUID}
     *
     * @param guid a dto guid
     * @return
     */
    public static EntityId of(GUID guid) {
        return new EntityId(guidToUuid(guid), guid);
    }

    /**
     * Constructs an entity from a string representation of a UUID.
     * @see UUID#fromString(String)
     *
     * @param uuid a string representation of a UUID
     * @return
     */
    public static EntityId of(String uuid) {
        return of(stringToUuid(uuid));
    }

    /**
     * Generates a new identifier based on UUIDv4
     * @return
     */
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
