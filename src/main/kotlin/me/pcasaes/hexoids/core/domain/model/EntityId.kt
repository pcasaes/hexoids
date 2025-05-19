package me.pcasaes.hexoids.core.domain.model;

import com.google.protobuf.ByteString;
import pcasaes.hexoids.proto.GUID;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Identifier immutable.
 * <p>
 * The identifier uses a {@link UUID} and holds its DTO {@link GUID} analog.
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
     *
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
     *
     * @param uuid a string representation of a UUID
     * @return
     * @see UUID#fromString(String)
     */
    public static EntityId of(String uuid) {
        return of(stringToUuid(uuid));
    }

    /**
     * Generates a new identifier based on UUIDv4
     *
     * @return
     */
    public static EntityId newId() {
        return of(UUID.randomUUID());
    }

    public static EntityId newId(Random ng) {
        // this code is copied and adapted from UUID

        byte[] randomBytes = new byte[16];
        ng.nextBytes(randomBytes);
        randomBytes[6] = (byte) (randomBytes[6] & 15);
        randomBytes[6] = (byte) (randomBytes[6] | 64);
        randomBytes[8] = (byte) (randomBytes[8] & 63);
        randomBytes[8] = (byte) (randomBytes[8] | 128);

        long msb = 0L;
        long lsb = 0L;

        int i;
        for (i = 0; i < 8; ++i) {
            msb = msb << 8 | (long) (randomBytes[i] & 255);
        }

        for (i = 8; i < 16; ++i) {
            lsb = lsb << 8 | (long) (randomBytes[i] & 255);
        }

        return of(new UUID(msb, lsb));
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
        ByteBuffer bb = ByteBuffer.wrap(guid.getGuid().toByteArray());
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    private static GUID uuidToGuid(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());


        return GUID_THREAD_SAFE_BUILDER
                .get()
                .clear()
                .setGuid(ByteString.copyFrom(bb.array()))
                .build();
    }

}
