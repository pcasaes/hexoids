package me.pcasaes.hexoids.core.domain.model

import com.google.protobuf.ByteString
import pcasaes.hexoids.proto.GUID
import java.nio.ByteBuffer
import java.util.Objects
import java.util.Random
import java.util.UUID
import java.util.function.Supplier
import java.util.logging.Logger

/**
 * Identifier immutable.
 *
 *
 * The identifier uses a [UUID] and holds its DTO [GUID] analog.
 */
class EntityId private constructor(
    private val id: UUID,
    private val guid: GUID
) {

    fun getId(): UUID {
        return id
    }

    fun getGuid(): GUID {
        return guid
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val entityId = other as EntityId
        return id == entityId.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        return this.id.toString()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(EntityId::class.java.getName())

        private val GUID_THREAD_SAFE_BUILDER: ThreadLocal<GUID.Builder> = ThreadLocal.withInitial<GUID.Builder>(
            Supplier { GUID.newBuilder() })

        /**
         * Constructs an EntityId from a [UUID]
         *
         * @param id a uuid
         * @return
         */
        fun of(id: UUID): EntityId {
            return EntityId(id, uuidToGuid(id))
        }

        /**
         * Constructs an EntityId from a DTO [GUID]
         *
         * @param guid a dto guid
         * @return
         */
        fun of(guid: GUID): EntityId {
            return EntityId(guidToUuid(guid), guid)
        }

        /**
         * Constructs an entity from a string representation of a UUID.
         *
         * @param uuid a string representation of a UUID
         * @return
         * @see UUID.fromString
         */
        fun of(uuid: String): EntityId {
            return of(stringToUuid(uuid))
        }

        /**
         * Generates a new identifier based on UUIDv4
         *
         * @return
         */
        fun newId(): EntityId {
            return of(UUID.randomUUID())
        }

        fun newId(ng: Random): EntityId {
            // this code is copied and adapted from UUID

            val randomBytes = ByteArray(16)
            ng.nextBytes(randomBytes)
            randomBytes[6] = (randomBytes[6].toInt() and 15).toByte()
            randomBytes[6] = (randomBytes[6].toInt() or 64).toByte()
            randomBytes[8] = (randomBytes[8].toInt() and 63).toByte()
            randomBytes[8] = (randomBytes[8].toInt() or 128).toByte()

            var msb = 0L
            var lsb = 0L
            var i: Int = 0
            while (i < 8) {
                msb = msb shl 8 or (randomBytes[i].toInt() and 255).toLong()
                ++i
            }

            i = 8
            while (i < 16) {
                lsb = lsb shl 8 or (randomBytes[i].toInt() and 255).toLong()
                ++i
            }

            return of(UUID(msb, lsb))
        }


        private fun stringToUuid(uuid: String): UUID {
            try {
                return UUID.fromString(uuid)
            } catch (ex: IllegalArgumentException) {
                LOGGER.severe { "Could not deserialize uuid: '$uuid'" }
                throw ex
            }
        }

        private fun guidToUuid(guid: GUID): UUID {
            val bb = ByteBuffer.wrap(guid.guid.toByteArray())
            val high = bb.getLong()
            val low = bb.getLong()
            return UUID(high, low)
        }

        private fun uuidToGuid(uuid: UUID): GUID {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)


            return GUID_THREAD_SAFE_BUILDER
                .get()
                .clear()
                .setGuid(ByteString.copyFrom(bb.array()))
                .build()
        }
    }
}
