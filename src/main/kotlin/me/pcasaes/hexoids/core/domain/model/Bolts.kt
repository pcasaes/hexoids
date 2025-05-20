package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.model.Bolt.Companion.create
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import pcasaes.hexoids.proto.BoltDivertedEventDto
import pcasaes.hexoids.proto.BoltFiredEventDto
import pcasaes.hexoids.proto.DirectedCommand
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.GUID
import pcasaes.hexoids.proto.LiveBoltListCommandDto
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * The collection of bolts. This collections only tracks bolts maintained
 * by this node.
 */
class Bolts : Iterable<Bolt> {
    private val activeBolts = HashMap<EntityId, Bolt>()
    private val publishableBoltDtos = HashMap<GUID, BoltFiredEventDto>()


    /**
     * Processes a bolt fired event.
     *
     * @param players
     * @param boltId
     * @param ownerPlayerId
     * @param x
     * @param y
     * @param angle
     * @param speed
     * @param startTimestamp
     * @return
     */
    fun fired(
        players: Players,
        boltId: EntityId,
        ownerPlayerId: EntityId,
        x: Float,
        y: Float,
        angle: Float,
        speed: Float,
        startTimestamp: Long,
        ttl: Int
    ): Bolt? {
        return if (activeBolts.containsKey(boltId)) {
            null
        } else {
            val bolt = create(players, boltId, ownerPlayerId, x, y, angle, speed, startTimestamp, ttl)
            activeBolts.put(bolt.id, bolt)
            bolt
        }
    }


    /**
     * Updates all tracked bolts updating their position vectors. Checks if they hit and are exhausted.
     *
     * @param timestamp the timestamp to update the players to.
     */
    fun fixedUpdate(timestamp: Long) {
        activeBolts
            .values
            .stream()
            .map { b -> b.updateTimestamp(timestamp) }
            .filter { it != null }
            .map { b -> b!!.tackleBoltExhaustion(timestamp) }
            .filter(Bolt::isActive)
            .forEach { b -> b.checkHits(timestamp) }

        cleanup()
    }

    /**
     * Returns an iterator of the collection.
     *
     * @return
     */
    override fun iterator(): MutableIterator<Bolt> {
        return activeBolts
            .values
            .iterator()
    }

    /**
     * Returns a stream of the collection.
     *
     * @return
     */
    fun stream(): Stream<Bolt> {
        return StreamSupport.stream(spliterator(), false)
    }

    /**
     * Handles bolt action domain events.
     *
     * @param domainEvent
     */
    fun consumeFromBoltActionTopic(domainEvent: DomainEvent) {
        if (domainEvent.event != null &&
            (domainEvent.event.hasBoltExhausted() ||
                    domainEvent.event.hasBoltFired() ||
                    domainEvent.event.hasBoltDiverted())
        ) {
            getClientEvents()
                .dispatch(
                    Dto.newBuilder()
                        .setEvent(domainEvent.event)
                        .build()
                )

            when {
                domainEvent.event.hasBoltFired() -> {
                    publishableBoltDtos.put(
                        domainEvent.event.getBoltFired().boltId,
                        domainEvent.event.getBoltFired()
                    )
                }

                domainEvent.event.hasBoltExhausted() -> {
                    publishableBoltDtos.remove(domainEvent.event.getBoltExhausted().boltId)
                }

                domainEvent.event.hasBoltDiverted() -> {
                    consumeBoltDiverted(domainEvent.event.getBoltDiverted())
                }
            }
        }
    }

    private fun consumeBoltDiverted(boltDiverted: BoltDivertedEventDto) {
        val boltId = boltDiverted.boltId
        val boltFiredEventDto = publishableBoltDtos[boltId]
        if (boltFiredEventDto != null) {
            val newTtl =
                boltFiredEventDto.ttl - (boltDiverted.divertTimestamp - boltFiredEventDto.startTimestamp).toInt()
            if (newTtl > 0) {
                publishableBoltDtos.put(
                    boltId,
                    BoltFiredEventDto.newBuilder()
                        .mergeFrom(boltFiredEventDto)
                        .setX(boltDiverted.x)
                        .setY(boltDiverted.y)
                        .setAngle(boltDiverted.angle)
                        .setSpeed(boltDiverted.speed)
                        .setStartTimestamp(boltDiverted.divertTimestamp)
                        .setTtl(newTtl)
                        .build()
                )
            } else {
                publishableBoltDtos.remove(boltId)
            }
        }
    }

    private fun cleanup() {
        val toRemove = activeBolts
            .entries
            .filter { e -> e.value.isExhausted }
            .map { it.key }

        toRemove
            .stream()
            .map { activeBolts.remove(it) }
            .forEach {
                if (it != null) {
                    Bolt.Companion.destroyObject(it)
                }
            }
    }

    fun requestListOfLiveBolts(requesterId: EntityId) {
        val liveBoltsList = LiveBoltListCommandDto.newBuilder()
            .addAllBolts(publishableBoltDtos.values)


        val builder = DirectedCommand.newBuilder()
            .setPlayerId(requesterId.getGuid())
            .setLiveBoltsList(liveBoltsList)

        getClientEvents().dispatch(
            Dto.newBuilder()
                .setDirectedCommand(builder)
                .build()
        )
    }

    /**
     * Return the total number of active bolts in the game.
     * Is weakly consistent and thread safe.
     *
     * @return
     */
    fun getTotalNumberOfActiveBolts(): Int {
        return this.activeBolts.size
    }

    companion object {
        /**
         * Creates the collection.
         *
         * @return
         */
        @JvmStatic
        fun create(): Bolts {
            return Bolts()
        }
    }
}
