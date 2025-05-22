package me.pcasaes.hexoids.core.domain.model.physics

import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics
import me.pcasaes.hexoids.core.domain.model.Bolts
import me.pcasaes.hexoids.core.domain.model.Clock
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.GameEvents
import me.pcasaes.hexoids.core.domain.model.GameObject
import me.pcasaes.hexoids.core.domain.model.Players
import me.pcasaes.hexoids.core.domain.utils.MathUtil
import me.pcasaes.hexoids.core.domain.vector.Vector2
import pcasaes.hexoids.proto.ClientPlatforms
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.Event
import pcasaes.hexoids.proto.MassCollapsedIntoBlackHoleEventDto
import pcasaes.hexoids.proto.MoveReason
import java.util.Locale
import java.util.Random
import java.util.function.Consumer
import java.util.function.LongPredicate
import java.util.logging.Logger
import kotlin.math.abs

class Blackhole private constructor(
    private val entityId: EntityId,
    private val center: Vector2,
    private val startTimestamp: Long, private val endTimestamp: Long,
    private val clock: Clock,
    private val players: Players,
    private val bolts: Bolts
) : LongPredicate {
    private val name: String
    private val eventHorizonRadius: Float
    private val gravityRadius: Float
    private val gravityImpulse: Float
    private val dampenFactor: Float
    private val rng: Random
    private val destroyProbability: Float

    init {
        this.eventHorizonRadius = Config.getBlackhole().getEventHorizonRadius()
        this.gravityRadius = Config.getBlackhole().getGravityRadius()
        this.gravityImpulse = Config.getBlackhole().getGravityImpulse()
        this.dampenFactor = Config.getBlackhole().getDampenFactor()
        this.rng = Random()
        this.destroyProbability = 1F - Config.getBlackhole().getTeleportProbability()

        val idStr = entityId.toString()
        val sbName = StringBuilder()
        for (c in idStr.uppercase(Locale.getDefault()).toCharArray()) {
            val firstChar = sbName.isEmpty()
            if (firstChar && c >= 'A' && c <= 'Z') {
                sbName.append((c.code + 6).toChar())
            } else if (!firstChar && c >= '0' && c <= '9') {
                sbName.append(c)
            }
            if (sbName.length > 3) {
                break
            }
        }
        sbName.append("*")
        this.name = sbName.toString()
    }

    private fun start(): Blackhole {
        val eventBuilder = Event.newBuilder()

        val massCollapsedIntoBlackHoleEventDto =
            MassCollapsedIntoBlackHoleEventDto.newBuilder()
                .setId(entityId.getGuid())
                .setX(center.x)
                .setY(center.y)
                .setStartTimestamp(startTimestamp)
                .setEndTimestamp(endTimestamp)
                .setName(name)
                .build()

        eventBuilder.setMassCollapsedIntoBlackHole(massCollapsedIntoBlackHoleEventDto)

        players.registerCurrentViewModifier(entityId) { currentViewBuilder ->
            if (clock.getTime() < endTimestamp) {
                currentViewBuilder.setBlackhole(massCollapsedIntoBlackHoleEventDto)
            }
        }

        GameEvents
            .getClientEvents()
            .dispatch(
                Dto.newBuilder()
                    .setEvent(eventBuilder)
                    .build()
            )
        return this
    }

    override fun test(timestamp: Long): Boolean {
        if (this.players.hasConnectedPlayers()) {
            players.getSpatialIndex()
                .search(center.x, center.y, center.x, center.y, gravityRadius)
                .forEach(Consumer { p ->
                    if (players.isConnected(p.id())) {
                        handleMove(p, timestamp)
                    }
                })
        }

        bolts.forEach { bolt -> handleMove(bolt, timestamp) }


        val exists = clock.getTime() < endTimestamp

        if (!exists) {
            GameMetrics.getBlackholeEvaporated().increment(ClientPlatforms.UNKNOWN)
            LOGGER.info { "Blackhole evaporated: $entityId" }
            players.unregisterCurrentViewModifier(entityId)
        }
        return exists
    }

    private fun accel(absMagnitude: Float): Float {
        val relDistance = absMagnitude / this.gravityRadius
        val invDistance = 1.0F - relDistance
        return MathUtil.cube(invDistance)
    }

    private fun handleMove(nearByGameObject: GameObject, timestamp: Long) {
        val distanceFromSingularity = center.minus(
            Vector2
                .fromXY(nearByGameObject.getX(), nearByGameObject.getY())
        )

        val absMagnitude = abs(distanceFromSingularity.magnitude)

        val isNotCenteredNorOutOfRange = absMagnitude < gravityRadius && absMagnitude > 0F
        if (isNotCenteredNorOutOfRange) {
            val sign = if (distanceFromSingularity.magnitude < 0F) -1 else 1

            val hitEventHorizon = absMagnitude <= this.eventHorizonRadius

            if (hitEventHorizon) {
                var destroyed = rng.nextFloat() <= this.destroyProbability
                if (!destroyed) {
                    val jumpX = rng.nextFloat()
                    val jumpY = rng.nextFloat()
                    destroyed = !nearByGameObject.teleport(jumpX, jumpY, timestamp, MoveReason.BLACKHOLE_TELEPORT)
                }

                if (destroyed) {
                    nearByGameObject.hazardDestroy(entityId, timestamp)
                    GameMetrics.getDestroyedByBlackhole().increment(nearByGameObject.getClientPlatform())
                } else {
                    GameMetrics.getMovedByBlackhole().increment(nearByGameObject.getClientPlatform())
                }
            } else {
                val acceleration = accel(absMagnitude)

                val move = Vector2
                    .fromAngleMagnitude(
                        distanceFromSingularity.angle,
                        sign * this.gravityImpulse * acceleration
                    )

                if (nearByGameObject.supportsInertialDampener()) {
                    nearByGameObject
                        .setDampenMovementFactorUntilNextFixedUpdate(1F / (acceleration * this.dampenFactor + 1F))
                }

                nearByGameObject.move(move.x, move.y, MoveReason.BLACKHOLE_PULL)
                GameMetrics.getMovedByBlackhole().increment(nearByGameObject.getClientPlatform())
            }
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(Blackhole::class.java.getName())

        fun massCollapsed(
            rng: Random,
            startTimestamp: Long, endTimestamp: Long,
            clock: Clock,
            players: Players,
            bolts: Bolts
        ): LongPredicate? {
            if (rng.nextInt(Config.getBlackhole().getGenesisProbabilityFactor()) > 0) {
                return null
            }

            val entityId = EntityId.newId(rng)
            val xp = rng.nextFloat()
            val yp = rng.nextFloat()

            // we must do this check AFTER consuming the RNG otherwise we introduce non-deterministic behavior
            if (endTimestamp < clock.getTime()) {
                return null
            }

            val blackhole = Blackhole(
                entityId,
                Vector2.fromXY(xp, yp),
                startTimestamp, endTimestamp - 10000L,
                clock,
                players,
                bolts
            ).start()

            GameMetrics.getMassCollapsedIntoBlackhole().increment(ClientPlatforms.UNKNOWN)
            LOGGER.info { "Mass collapsed. id = " + blackhole.entityId + ", name = " + blackhole.name + ", center = " + blackhole.center + ",  start = " + blackhole.startTimestamp + ", end = " + blackhole.endTimestamp }

            return blackhole
        }
    }
}
