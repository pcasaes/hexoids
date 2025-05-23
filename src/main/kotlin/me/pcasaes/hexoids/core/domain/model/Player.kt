package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics
import me.pcasaes.hexoids.core.domain.model.Bolt.Companion.isExpired
import me.pcasaes.hexoids.core.domain.utils.TrigUtil
import me.pcasaes.hexoids.core.domain.vector.PositionVector
import me.pcasaes.hexoids.core.domain.vector.PositionVector.Configuration.AtBoundsOptions
import me.pcasaes.hexoids.core.domain.vector.Vector2
import pcasaes.hexoids.proto.BoltFiredEventDto
import pcasaes.hexoids.proto.BoltsAvailableCommandDto
import pcasaes.hexoids.proto.ClientPlatforms
import pcasaes.hexoids.proto.DirectedCommand
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.Event
import pcasaes.hexoids.proto.JoinCommandDto
import pcasaes.hexoids.proto.MoveReason
import pcasaes.hexoids.proto.PlayerDestroyedEventDto
import pcasaes.hexoids.proto.PlayerDto
import pcasaes.hexoids.proto.PlayerJoinedEventDto
import pcasaes.hexoids.proto.PlayerLeftEventDto
import pcasaes.hexoids.proto.PlayerMovedEventDto
import pcasaes.hexoids.proto.PlayerSpawnedEventDto
import java.util.Objects
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * A model representation of the player. This model conflates player and ship information.
 * As this model becomes richer it might be a good idea to split the two.
 *
 *
 * Most action methods have a corresponding process method in the past tense, ex:
 * fire
 * fired
 *
 *
 * The action method will generate a domain event which will be distributed to all nodes and processed in
 * the corresponding process method.
 */
interface Player : GameObject {
    fun id(): EntityId

    /**
     * Fires a bolt. Will generate a [BoltFiredEventDto] domain event
     */
    fun fire()

    /**
     * Processes a bolt fired by this player.
     *
     * @param event
     */
    fun fired(event: BoltFiredEventDto)

    /**
     * Returns the number of still active bolts fired by this player
     *
     * @return
     */
    fun getActiveBoltCount(): Int

    /**
     * Returns true if this player's id matches the paramer
     *
     * @param playerId
     * @return
     */
    fun hasId(playerId: EntityId): Boolean

    /**
     * Generates a [PlayerDto] from the player's current states
     *
     * @return
     */
    fun toDtoIfJoined(): PlayerDto?

    /**
     * The player will join the game
     *
     * @param command
     */
    fun join(command: JoinCommandDto)

    /**
     * Processes a player joined game event
     *
     * @param event
     */
    fun joined(event: PlayerJoinedEventDto)

    /**
     * Move the player by a vector
     *
     * @param moveX vector x component
     * @param moveY vector y component
     * @param angle fire direction
     */
    fun move(moveX: Float, moveY: Float, angle: Float?)

    /**
     * Processes a player moved event
     *
     * @param event
     */
    fun moved(event: PlayerMovedEventDto)

    /**
     * Leaves the game
     */
    fun leave()

    /**
     * Processes a player left game event.
     */
    fun left()

    /**
     * Set's the dampen fa
     *
     * @param value
     */
    fun setFixedInertialDampenFactor(value: Float)

    /**
     * Will return true if the player's ship collides with the supplied [PositionVector]
     *
     * @param velocityVector
     * @param collisionRadius
     * @return
     */
    fun collision(velocityVector: PositionVector, collisionRadius: Float): Boolean

    /**
     * This informed player destroys this player
     *
     * @param byPlayerId
     * @param timestamp
     */
    fun destroy(byPlayerId: EntityId, timestamp: Long)

    /**
     * Processes the destroyed player domain event
     *
     * @param event
     */
    fun destroyed(event: PlayerDestroyedEventDto)

    /**
     * Called whenever this player's bolts are exhausted
     */
    fun boltExhausted()

    /**
     * Spawns the player
     */
    fun spawn()

    /**
     * Processes the player spawned domain event
     *
     * @param event
     */
    fun spawned(event: PlayerSpawnedEventDto)

    /**
     * Periodically called if the player has not spawned for a certain amount of time.
     * This will cause the player to leave the game.
     */
    fun expungeIfStalled()

    /**
     * Updates the player's vector position up tot he supplied timestamp.
     *
     * @param timestamp
     */
    fun fixedUpdate(timestamp: Long)

    private class Implementation(
        private val id: EntityId,
        private val players: Players,
        private val bolts: Bolts,
        private val barriers: Barriers,
        private val clock: Clock,
        private val scoreBoard: ScoreBoard
    ) : Player {
        private var name: String? = null

        private var clientPlatform: ClientPlatforms = ClientPlatforms.UNKNOWN

        private var ship: Int

        private var spawned = false

        private var lastSpawnOrUnspawnTimestamp: Long

        private var previousAngle = 0F

        private var angle = 0F

        private var movedTimestamp: Long = 0

        private var spawnedTimestamp: Long = 0

        private var liveBolts = 0

        private val resetPosition: ResetPosition

        private val position: PositionVector

        private val playerPositionConfiguration: PlayerPositionConfiguration

        private var fixedInertialDampenFactor = 1F

        private val lastMoveReasons = HashSet<MoveReason>()

        init {
            this.ship = RNG.nextInt(12)
            this.lastSpawnOrUnspawnTimestamp = clock.getTime()
            this.resetPosition = ResetPosition.create(Config.getPlayerResetPosition())
            this.playerPositionConfiguration = PlayerPositionConfiguration()
            this.position = PositionVector.of(
                0F,
                0F,
                0F,
                0F,
                0,
                playerPositionConfiguration
            )
        }

        override fun id(): EntityId {
            return this.id
        }

        override fun setDampenMovementFactorUntilNextFixedUpdate(factor: Float) {
            this.playerPositionConfiguration.setDampenFactor(factor)
        }

        private fun setSpawned(spawned: Boolean) {
            this.spawned = spawned
            this.lastSpawnOrUnspawnTimestamp = clock.getTime()
        }

        private fun getFiredBoltVector(boltSpeed: Float, firedTime: Long): Vector2 {
            val boltVector = Vector2.fromAngleMagnitude(this.angle, boltSpeed)

            val currentShipVector = this.position.getVectorAt(firedTime)
            var projection = currentShipVector
                .projection(boltVector)

            val rejection = currentShipVector
                .minus(projection)
                .scale(Config.getBoltInertiaRejectionScale())

            projection = if (boltVector.sameDirection(projection)) {
                projection.scale(Config.getBoltInertiaProjectionScale())
            } else {
                projection.scale(Config.getBoltInertiaNegativeProjectionScale())
            }
            projection = projection
                .scale(Config.getBoltInertiaProjectionScale())

            return boltVector.add(rejection).add(projection)
        }

        override fun fire() {
            if (!spawned) {
                return
            }

            val now = clock.getTime()

            var boltSpeed = Config.getBoltSpeed()
            val boltAngle: Float

            val boltVector: Vector2
            if (!Config.isBoltInertiaEnabled) {
                boltAngle = angle
                boltVector = Vector2.fromAngleMagnitude(boltAngle, boltSpeed)
            } else {
                boltVector = getFiredBoltVector(boltSpeed, now)
                boltSpeed = boltVector.magnitude
                boltAngle = boltVector.angle
            }


            val positionAtNow = Vector2.fromXY(
                position.getXat(now),
                position.getYat(now)
            )

            val ttl = calculateBoltTll(positionAtNow, boltVector, boltSpeed)


            val boltId = EntityId.newId()
            GameEvents.getDomainEvents()
                .dispatch(
                    DomainEvent.create(
                        GameTopic.BOLT_LIFECYCLE_TOPIC.name,
                        boltId.getId(),
                        Event.newBuilder()
                            .setPlayerFired(
                                BoltFiredEventDto.newBuilder()
                                    .setBoltId(boltId.getGuid())
                                    .setOwnerPlayerId(id.getGuid())
                                    .setX(positionAtNow.x)
                                    .setY(positionAtNow.y)
                                    .setAngle(boltAngle)
                                    .setSpeed(boltSpeed)
                                    .setStartTimestamp(now)
                                    .setTtl(ttl)
                            )
                            .build()
                    )
                )
        }

        private fun calculateBoltTll(positionAtNow: Vector2, boltVector: Vector2, boltSpeed: Float): Int {
            val maxTtl = Config.getBoltMaxDuration()

            val moveDelta = Vector2.calculateMoveDelta(boltVector, Float.Companion.MIN_VALUE, maxTtl.toLong())

            val positionAtEnd = positionAtNow.add(moveDelta)

            var magnitudeForTtl = -1F

            for (barrier in barriers
                .search(
                    positionAtNow.x,
                    positionAtNow.y,
                    positionAtEnd.x,
                    positionAtEnd.y,
                    SHIP_LENGTH_TIMES_10
                )) {
                val intersection = Vector2.intersectedWith(positionAtNow, positionAtEnd, barrier.to, barrier.from)
                if (intersection != null) {
                    if (magnitudeForTtl == -1F) {
                        magnitudeForTtl = intersection.minus(positionAtNow).magnitude
                    } else {
                        val m = intersection.minus(positionAtNow).magnitude
                        if (m < magnitudeForTtl) {
                            magnitudeForTtl = m
                        }
                    }
                }
            }

            if (magnitudeForTtl != -1F) {
                return min(maxTtl, (1000F * magnitudeForTtl / boltSpeed).toInt())
            }
            return maxTtl
        }

        override fun fired(event: BoltFiredEventDto) {
            val now = clock.getTime()
            if (isExpired(now, event.startTimestamp, event.ttl)) {
                val bolt = toBolt(event)
                if (bolt != null) {
                    bolt.updateTimestamp(now)
                    bolt.tackleBoltExhaustion(now)
                }
            } else if (this.liveBolts < Config.getMaxBolts()) {
                val bolt = toBolt(event)
                if (bolt != null) {
                    firedNew(event, bolt)
                }
            }
        }

        private fun firedNew(event: BoltFiredEventDto, bolt: Bolt) {
            this.liveBolts++
            bolt.fire(event)
            liveBoltsChanged()
            GameMetrics.getBoltFired().increment(this.clientPlatform)
        }

        private fun toBolt(event: BoltFiredEventDto): Bolt? {
            return this.bolts.fired(
                players,
                EntityId.of(event.boltId),
                this.id,
                event.x,
                event.y,
                event.angle,
                event.speed,
                event.startTimestamp,
                event.ttl
            )
        }


        override fun getActiveBoltCount(): Int {
            return this.liveBolts
        }

        override fun hasId(playerId: EntityId): Boolean {
            return id == playerId
        }

        override fun toDtoIfJoined(): PlayerDto? {
            return if (!this.isJoined) {
                null
            } else {
                PlayerDto.newBuilder()
                    .setPlayerId(id.getGuid())
                    .setShip(ship)
                    .setX(position.x)
                    .setY(position.y)
                    .setAngle(angle)
                    .setSpawned(spawned)
                    .setName(name)
                    .build()
            }
        }

        private fun resetPosition(resetTime: Long) {
            position.initialized(resetPosition.getNextX(), resetPosition.getNextY(), resetTime)
            this.angle = 0F
        }

        private fun setName(name: String) {
            var n = name
            n = n.ifEmpty { id.getId().toString() }

            if (n.length > Config.getPlayerNameLength()) {
                n = n.substring(0, Config.getPlayerNameLength())
            }
            this.name = n
        }

        fun setClientPlatform(clientPlatform: ClientPlatforms) {
            this.clientPlatform = clientPlatform
        }

        override fun teleport(x: Float, y: Float, timestamp: Long, moveReason: MoveReason): Boolean {
            position.teleport(x, y, timestamp)
            lastMoveReasons.add(moveReason)

            return true
        }

        override fun join(command: JoinCommandDto) {
            setName(command.name)
            setClientPlatform(command.getClientPlatform())

            GameEvents.getDomainEvents().dispatch(
                DomainEvent
                    .create(
                        GameTopic.JOIN_GAME_TOPIC.name,
                        this.id.getId(),
                        Event.newBuilder()
                            .setPlayerJoined(
                                PlayerJoinedEventDto.newBuilder()
                                    .setPlayerId(id.getGuid())
                                    .setShip(ship)
                                    .setName(name)
                                    .setClientPlatform(clientPlatform)
                            )
                            .build()
                    )
            )
        }

        override fun joined(event: PlayerJoinedEventDto) {
            this.name = event.name
            this.clientPlatform = event.getClientPlatform()
            this.ship = event.ship
            GameEvents
                .getClientEvents()
                .dispatch(
                    Dto.newBuilder()
                        .setEvent(
                            Event.newBuilder()
                                .setPlayerJoined(
                                    PlayerJoinedEventDto.newBuilder()
                                        .mergeFrom(event)
                                        .clearClientPlatform() //let's not publish to all clients user data
                                )
                        ).build()
                )
            GameMetrics.getPlayerJoined().increment(this.clientPlatform)
        }

        override fun setFixedInertialDampenFactor(value: Float) {
            this.fixedInertialDampenFactor = min(1F, value)
        }

        override fun move(moveX: Float, moveY: Float, moveReason: MoveReason?) {
            move(moveX, moveY, null as Float?)
            if (moveReason != null) {
                this.lastMoveReasons.add(moveReason)
            }
        }

        override fun move(moveX: Float, moveY: Float, angle: Float?) {
            if (!this.spawned) {
                return
            }

            if (angle != null) {
                this.previousAngle = this.angle
                this.angle = TrigUtil.limitRotation(this.angle, angle, Config.getPlayerMaxAngle())
            }

            position.scheduleMove(moveX, moveY)
        }

        override fun moved(event: PlayerMovedEventDto) {
            val ts = event.timestamp
            if (ts > this.movedTimestamp) {
                this.movedTimestamp = ts
                movedOrSpawned(event, null)
            }
        }

        private fun movedOrSpawned(movedEvent: PlayerMovedEventDto, spawnedEvent: PlayerSpawnedEventDto?) {
            this.position.moved(
                movedEvent.x,
                movedEvent.y,
                movedEvent.thrustAngle,
                movedEvent.velocity,
                movedEvent.timestamp
            )
            this.angle = movedEvent.angle

            val eventBuilder = Event.newBuilder()
            if (spawnedEvent != null) {
                eventBuilder.setPlayerSpawned(spawnedEvent)
            } else {
                eventBuilder.setPlayerMoved(movedEvent)
            }
            GameEvents
                .getClientEvents()
                .dispatch(
                    Dto.newBuilder()
                        .setEvent(eventBuilder)
                        .build()
                )
        }

        private fun fireMoveDomainEvent(eventTime: Long) {
            GameEvents.getDomainEvents().dispatch(
                DomainEvent.create(
                    GameTopic.PLAYER_ACTION_TOPIC.name,
                    this.id.getId(),
                    Event.newBuilder()
                        .setPlayerMoved(
                            PlayerMovedEventDto.newBuilder()
                                .setPlayerId(id.getGuid())
                                .setX(position.x)
                                .setY(position.y)
                                .setAngle(angle)
                                .setThrustAngle(position.velocity.angle)
                                .setVelocity(position.velocity.magnitude)
                                .setTimestamp(eventTime)
                                .addAllReasons(lastMoveReasons)
                                .setInertialDampenFactor(playerPositionConfiguration.getDampenFactor())

                        ).build()
                )
            )

            lastMoveReasons.clear()
        }

        override fun leave() {
            GameEvents.getDomainEvents().dispatch(DomainEvent.delete(GameTopic.JOIN_GAME_TOPIC.name, this.id.getId()))
            GameEvents.getDomainEvents()
                .dispatch(DomainEvent.delete(GameTopic.PLAYER_ACTION_TOPIC.name, this.id.getId()))
        }

        override fun left() {
            scoreBoard.resetScore(this.id)
            GameEvents.getClientEvents().dispatch(
                Dto.newBuilder()
                    .setEvent(
                        Event.newBuilder()
                            .setPlayerLeft(PlayerLeftEventDto.newBuilder().setPlayerId(id.getGuid()))
                    )
                    .build()
            )
            GameMetrics.getPlayerLeft().increment(this.clientPlatform)
            this.spawned = false
        }

        override fun collision(velocityVector: PositionVector, collisionRadius: Float): Boolean {
            if (!this.spawned) {
                return false
            }
            return velocityVector.intersectedWith(this.position, collisionRadius)
        }

        override fun destroy(byPlayerId: EntityId, timestamp: Long) {
            if (spawned) {
                hazardDestroy(byPlayerId, timestamp)
                this.scoreBoard.updateScore(byPlayerId, 1)
            }
        }

        override fun hazardDestroy(hazardId: EntityId, timestamp: Long) {
            if (spawned) {
                GameEvents.getDomainEvents().dispatch(
                    DomainEvent.create(
                        GameTopic.PLAYER_ACTION_TOPIC.name,
                        this.id.getId(),
                        Event.newBuilder()
                            .setPlayerDestroyed(
                                PlayerDestroyedEventDto.newBuilder()
                                    .setPlayerId(this.id.getGuid())
                                    .setDestroyedById(hazardId.getGuid())
                                    .setDestroyedTimestamp(timestamp)
                            )
                            .build()
                    )
                )
            }
        }

        override fun destroyed(event: PlayerDestroyedEventDto) {
            setSpawned(false)
            this.scoreBoard.resetScore(this.id)
            GameEvents.getClientEvents().dispatch(
                Dto.newBuilder()
                    .setEvent(Event.newBuilder().setPlayerDestroyed(event))
                    .build()
            )
            GameMetrics.getPlayerDestroyed().increment(this.clientPlatform)
        }

        override fun boltExhausted() {
            this.liveBolts = max(0, liveBolts - 1)
            liveBoltsChanged()
            GameMetrics.getBoltExhausted().increment(this.clientPlatform)
        }

        private fun liveBoltsChanged() {
            if (players.isConnected(id)) {
                val dto = BoltsAvailableCommandDto.newBuilder()
                    .setAvailable(max(0, Config.getMaxBolts() - this.liveBolts))

                val builder = DirectedCommand.newBuilder()
                    .setPlayerId(id.getGuid())
                    .setBoltsAvailable(dto)

                GameEvents.getClientEvents().dispatch(
                    Dto.newBuilder()
                        .setDirectedCommand(builder)
                        .build()
                )
            }
        }

        override fun spawn() {
            if (!this.spawned) {
                setSpawned(true)
                val now = clock.getTime()
                resetPosition(now)
                GameEvents.getDomainEvents().dispatch(
                    DomainEvent.create(
                        GameTopic.PLAYER_ACTION_TOPIC.name,
                        this.id.getId(),
                        Event.newBuilder()
                            .setPlayerSpawned(
                                PlayerSpawnedEventDto.newBuilder()
                                    .setLocation(
                                        PlayerMovedEventDto.newBuilder()
                                            .setPlayerId(id.getGuid())
                                            .setX(position.x)
                                            .setY(position.y)
                                            .setAngle(angle)
                                            .setThrustAngle(position.velocity.angle)
                                            .setTimestamp(now)
                                            .setInertialDampenFactor(playerPositionConfiguration.getDampenFactor())
                                    )
                            )
                            .build()
                    )
                )
            }
        }

        override fun spawned(event: PlayerSpawnedEventDto) {
            val ts = event.location.timestamp
            if (ts > this.spawnedTimestamp) {
                this.spawnedTimestamp = ts
                setSpawned(true)
                this.position.initialized(
                    event.location.x,
                    event.location.y,
                    0L
                )
                movedOrSpawned(event.location, event)
                GameMetrics.getPlayerSpawned().increment(this.clientPlatform)
            }
        }

        override fun expungeIfStalled() {
            if ((!spawned || !this.isJoined) && clock.getTime() - this.lastSpawnOrUnspawnTimestamp > Config
                    .getExpungeSinceLastSpawnTimeout()
            ) {
                leave()
                GameMetrics.getPlayerStalled().increment(this.clientPlatform)
            }
        }

        override fun fixedUpdate(timestamp: Long) {
            val x = position.x
            val y = position.y
            this.position.update(timestamp)
            val angleChanged = this.previousAngle != this.angle
            if (x != position.x || y != position.y) {
                tackleBarrierHit()
                fireMoveDomainEvent(timestamp)
            } else if (angleChanged) {
                fireMoveDomainEvent(timestamp)
            }
            if (angleChanged) {
                this.previousAngle = this.angle
            }
            this.playerPositionConfiguration.setDampenFactor(this.fixedInertialDampenFactor)
        }

        private fun tackleBarrierHit() {
            if (!position.noMovement()) {
                for (barrier in barriers
                    .search(
                        position.previousX,
                        position.previousY,
                        position.x,
                        position.y,
                        SHIP_LENGTH_TIMES_10
                    )) {
                    val intersection = position.intersectedWith(barrier.to, barrier.from, SHIP_HALF_LENGTH)
                    if (intersection != null) {
                        position.reflect(intersection, barrier.normal, SHIP_HALF_LENGTH, 0.5F)
                    }
                }
            }
        }

        private val isJoined: Boolean
            get() = this.name != null

        override fun getX(): Float {
            return this.position.x
        }

        override fun getY(): Float {
            return this.position.y
        }

        override fun getClientPlatform(): ClientPlatforms {
            return clientPlatform
        }

        override fun supportsInertialDampener(): Boolean {
            return true
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as Implementation
            return id == that.id
        }

        override fun hashCode(): Int {
            return Objects.hash(id)
        }

        class PlayerPositionConfiguration : PositionVector.Configuration {
            private var dampenFactor = 1F

            fun setDampenFactor(dampenFactor: Float) {
                this.dampenFactor = dampenFactor
            }

            fun getDampenFactor(): Float {
                return dampenFactor
            }

            override fun atBounds(): AtBoundsOptions {
                return AtBoundsOptions.BOUNCE
            }

            override fun maxMagnitude(): Double? {
                return Config.getPlayerMaxMove().toDouble()
            }

            override fun dampenMagnitudeCoefficient(): Float {
                return Config.getInertiaDampenCoefficient() * dampenFactor
            }
        }

        companion object {
            private const val SHIP_LENGTH = 0.003F

            private const val SHIP_HALF_LENGTH: Float = SHIP_LENGTH / 2F

            private const val SHIP_LENGTH_TIMES_10: Float = SHIP_LENGTH * 10F

            private val RNG = Random()
        }
    }

    companion object {
        /**
         * Creates an instanceof a player
         *
         * @param id         the player's id
         * @param players    the collection of all players
         * @param bolts      the collection of all bolts
         * @param barriers   the collection of all barriers
         * @param clock      the game clock.
         * @param scoreBoard scoreboard
         * @return an new instance of player
         */
        fun create(
            id: EntityId,
            players: Players,
            bolts: Bolts,
            barriers: Barriers,
            clock: Clock,
            scoreBoard: ScoreBoard
        ): Player {
            return Implementation(id, players, bolts, barriers, clock, scoreBoard)
        }
    }
}
