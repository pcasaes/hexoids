package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.vector.PositionVector
import pcasaes.hexoids.proto.BoltDivertedEventDto
import pcasaes.hexoids.proto.BoltExhaustedEventDto
import pcasaes.hexoids.proto.BoltFiredEventDto
import pcasaes.hexoids.proto.Event
import pcasaes.hexoids.proto.MoveReason
import java.util.ArrayDeque
import java.util.Queue
import kotlin.math.max

/**
 * A model representation of a bolt.
 */
class Bolt private constructor(
    private val players: Players,
    var id: EntityId,
    private var ownerPlayerId: EntityId,
    val positionVector: PositionVector,
    private var timestamp: Long,
    private var ttl: Int
) : GameObject {
    private var startTimestamp: Long

    /**
     * Returns true if exhausted.
     *
     * @return
     */
    var isExhausted: Boolean = false
        private set

    private var firedEventDto: BoltFiredEventDto? = null

    init {
        this.startTimestamp = this.timestamp
    }

    /**
     * Return true if the id matches this bolt's id.
     *
     * @param id
     * @return
     */
    fun `is`(id: EntityId?): Boolean {
        return this.id == id
    }

    fun fire(event: BoltFiredEventDto) {
        this.firedEventDto = event
        GameEvents.getDomainEvents()
            .dispatch(
                DomainEvent
                    .create(
                        GameTopic.BOLT_ACTION_TOPIC.name,
                        this.id.getId(),
                        Event.newBuilder()
                            .setBoltFired(event)
                            .build()
                    )
            )
    }

    /**
     * Updates this bolt's internal timestamp.
     *
     * @param timestamp
     * @return return empty if the timestamp hasn't changed
     */
    fun updateTimestamp(timestamp: Long): Bolt? {
        val elapsed = max(0L, timestamp - this.timestamp)
        return if (elapsed > 0L) {
            this.timestamp = timestamp
            this.positionVector.update(timestamp)
            tackleDiverted(timestamp)

            this
        } else {
            null
        }
    }

    private fun tackleDiverted(timestamp: Long) {
        if (!this.positionVector.movedByScheduledMove()) {
            return
        }

        val eventDto = this.firedEventDto
        if (eventDto != null) {
            GameEvents.getDomainEvents()
                .dispatch(
                    DomainEvent
                        .create(
                            GameTopic.BOLT_ACTION_TOPIC.name,
                            this.id.getId(),
                            Event.newBuilder()
                                .setBoltDiverted(
                                    BoltDivertedEventDto
                                        .newBuilder()
                                        .setBoltId(eventDto.boltId)
                                        .setAngle(positionVector.getVelocity().angle)
                                        .setSpeed(positionVector.getVelocity().magnitude)
                                        .setX(positionVector.getX())
                                        .setY(positionVector.getY())
                                        .setDivertTimestamp(timestamp)
                                        .build()
                                )
                                .build()
                        )
                )
        }
    }

    /**
     * If bolt is out of bounds or expired will be marked as exhausted
     *
     * @param timestamp
     * @return
     */
    fun tackleBoltExhaustion(timestamp: Long): Bolt {
        if (positionVector.isOutOfBounds() || this.isExpired) {
            exhaust(timestamp)
        }
        return this
    }

    override fun hazardDestroy(hazardId: EntityId, timestamp: Long) {
        exhaust(timestamp)
    }

    private fun exhaust(timestamp: Long) {
        this.isExhausted = true
        GameEvents.getDomainEvents().dispatch(generateExhaustedEvent(timestamp))
    }

    override fun move(moveX: Float, moveY: Float, moveReason: MoveReason?) {
        if (this.isExhausted || positionVector.isOutOfBounds() || this.isExpired) {
            return
        }

        positionVector.scheduleMove(moveX, moveY)
    }

    private val isExpired: Boolean
        get() = isExpired(this.timestamp, this.startTimestamp, this.ttl)

    val isActive: Boolean
        /**
         * The inverse of isExhausted.
         *
         * @return
         */
        get() = !this.isExhausted

    /**
     * Checks if this bolt hit a player
     */
    fun checkHits(timestamp: Long) {
        if (!this.isExhausted) {
            this.players
                .getSpatialIndex()
                .search(
                    this.positionVector.getPreviousX(), this.positionVector.getPreviousY(),
                    this.positionVector.getX(), this.positionVector.getY(),
                    Config.getBoltCollisionIndexSearchDistance()
                )
                .forEach { p -> hit(p, timestamp) }
        }
    }

    private fun hit(player: Player, timestamp: Long) {
        val isHit = !player.hasId(ownerPlayerId) &&
                player.collision(positionVector, Config.getBoltCollisionRadius())

        if (isHit) {
            player.destroy(this.ownerPlayerId, timestamp)
            if (!this.isExhausted) {
                this.isExhausted = true

                GameEvents.getDomainEvents().dispatch(
                    generateExhaustedEvent(timestamp)
                )
            }
        }
    }

    private fun generateExhaustedEvent(timestamp: Long): DomainEvent {
        return DomainEvent
            .create(
                GameTopic.BOLT_ACTION_TOPIC.name,
                this.id.getId(),
                Event.newBuilder()
                    .setBoltExhausted(
                        BoltExhaustedEventDto.newBuilder()
                            .setBoltId(id.getGuid())
                            .setOwnerPlayerId(ownerPlayerId.getGuid())
                            .setTimestamp(timestamp)
                    )
                    .build()
            )
    }


    fun isOwnedBy(playerId: EntityId?): Boolean {
        return this.ownerPlayerId == playerId
    }

    override fun getX(): Float {
        return positionVector.getX()
    }

    override fun getY(): Float {
        return positionVector.getY()
    }


    companion object {
        private val POOL: Queue<Bolt> = ArrayDeque<Bolt>(1024)

        /**
         * Creates a bolt
         *
         * @param players
         * @param boltId
         * @param ownerPlayerId
         * @param x
         * @param y
         * @param angle
         * @param speed
         * @param startTimestamp
         * @param ttl
         * @return
         */
        fun create(
            players: Players,
            boltId: EntityId,
            ownerPlayerId: EntityId,
            x: Float,
            y: Float,
            angle: Float,
            speed: Float,
            startTimestamp: Long,
            ttl: Int
        ): Bolt {
            val bolt = POOL.poll()
            return if (bolt == null) {
                Bolt(
                    players,
                    boltId,
                    ownerPlayerId,
                    PositionVector.of(
                        x,
                        y,
                        angle,
                        speed,
                        startTimestamp
                    ),
                    startTimestamp,
                    ttl
                )
            } else {
                bolt.id = boltId
                bolt.ownerPlayerId = ownerPlayerId
                bolt.startTimestamp = startTimestamp
                bolt.ttl = ttl
                bolt.timestamp = startTimestamp
                bolt.isExhausted = false
                bolt.positionVector.initialized(x, y, angle, speed, startTimestamp)

                bolt
            }
        }

        fun destroyObject(bolt: Bolt) {
            // FIXME: we need to reset a bolt to not have ownership when returned to the pool
            //bolt.id = null
            //bolt.ownerPlayerId = null
            POOL.offer(bolt)
        }

        fun isExpired(now: Long, startTimestamp: Long, ttl: Int): Boolean {
            return now - startTimestamp > ttl
        }
    }
}
