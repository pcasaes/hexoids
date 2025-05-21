package me.pcasaes.hexoids.core.domain.model

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory.Companion.factory
import me.pcasaes.hexoids.core.domain.model.Bolts.Companion.create
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.newId
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getDomainEvents
import me.pcasaes.hexoids.core.domain.vector.PositionVector
import me.pcasaes.hexoids.core.domain.vector.PositionVector.Companion.of
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.JoinCommandDto
import pcasaes.hexoids.proto.MoveReason
import pcasaes.hexoids.proto.PlayerJoinedEventDto
import pcasaes.hexoids.proto.PlayerMovedEventDto
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.cos


class PlayerTest {
    private val clock = mockk<Clock>(relaxed = true)

    private val scoreBoard = mockk<ScoreBoard>(relaxed = true)

    private val game = mockk<Game>(relaxed = true)

    private val barriers = mockk<Barriers>(relaxed = true)

    private val physicsQueue = mockk<PhysicsQueueEnqueue>(relaxed = true)

    private lateinit var bolts: Bolts

    private lateinit var players: Players

    @BeforeEach
    fun setup() {

        this.bolts = create()
        this.players = Players.create(bolts, clock, scoreBoard, barriers, physicsQueue, factory())
        Assertions.assertEquals(0, this.players.getTotalNumberOfPlayers())
        Assertions.assertEquals(0, this.players.getNumberOfConnectedPlayers())

        every { game.getClock() } returns clock

        every { game.getScoreBoard() } returns scoreBoard

        every { game.getPlayers() } returns players

        every { game.getBolts() } returns bolts

        every { barriers.iterator() } returns emptyList<Barrier>().iterator()

        every { barriers.spliterator() } returns emptyList<Barrier>().spliterator()

        every { barriers.search(any(), any(), any(), any(), any()) } returns emptyList()

        GameTopic.setGame(game)

        getClientEvents().registerEventDispatcher { }
        getDomainEvents().registerEventDispatcher { domainEvent ->
            GameTopic.valueOf(domainEvent.topic!!).consume(
                domainEvent
            )
        }


        every { clock.getTime() } returns 0L

        Config.setPlayerMaxMove(1f)
        Config.setPlayerNameLength(7)
        Config.setMinMove(0.000000001f)
        Config.setPlayerMaxAngleDivisor(0.5f)
        Config.setBoltInertiaEnabled(false)
        Config.setUpdateFrequencyInMillis(50L)
    }

    @Test
    fun testCreate() {
        val one = newId()
        val two = newId()
        val player = this.players.createOrGet(one)

        Assertions.assertTrue(
            this.players
                .any { p -> p === player })

        Assertions.assertTrue(player.hasId(one))
        Assertions.assertFalse(player.hasId(two))

        Assertions.assertSame(player, this.players.get(one))

        Assertions.assertNotSame(player, this.players.get(two))

        Assertions.assertEquals(1, this.players.getTotalNumberOfPlayers())
        Assertions.assertEquals(0, this.players.getNumberOfConnectedPlayers())
    }

    @Test
    fun testJoin() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()
        this.players.createOrGet(one).join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        this.players.connected(one)
        val event = eventReference.get()!!.getEvent().getPlayerJoined()

        Assertions.assertNotNull(event)
        Assertions.assertEquals(one.getGuid(), event!!.playerId)
        Assertions.assertEquals("one", event.name)

        Assertions.assertEquals(1, this.players.getTotalNumberOfPlayers())
        Assertions.assertEquals(1, this.players.getNumberOfConnectedPlayers())
    }

    @Test
    fun testJoinNoName() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()
        this.players.createOrGet(one).join(
            JoinCommandDto
                .newBuilder()
                .build()
        )
        this.players.connected(one)
        val event = eventReference.get()!!.getEvent().getPlayerJoined()

        Assertions.assertNotNull(event)
        Assertions.assertEquals(one.getGuid(), event!!.playerId)
        Assertions.assertEquals(one.toString().substring(0, Config.getPlayerNameLength()), event.name)

        Assertions.assertEquals(1, this.players.getTotalNumberOfPlayers())
        Assertions.assertEquals(1, this.players.getNumberOfConnectedPlayers())
    }

    @Test
    fun testJoined() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()

        this.players.joined(
            PlayerJoinedEventDto.newBuilder()
                .setPlayerId(one.getGuid())
                .setShip(5)
                .build()
        )
        val event = eventReference.get()!!.getEvent().getPlayerJoined()

        Assertions.assertNotNull(event)
        Assertions.assertEquals(one.getGuid(), event!!.playerId)
        Assertions.assertEquals(5, event.ship)

        Assertions.assertEquals(1, this.players.getTotalNumberOfPlayers())
    }

    @Test
    fun testLeave() {
        val dtos = ArrayList<Dto>()
        getClientEvents().registerEventDispatcher { e -> dtos.add(e) }

        val one = newId()
        val player = this.players.createOrGet(one)
        this.players.connected(one)
        player.leave()

        val events = dtos
            .filter { it.hasEvent() }
            .map { it.getEvent() }

        Assertions.assertEquals(1, events.size)

        val event = events[0]
        Assertions.assertEquals(one.getGuid(), event.getPlayerLeft().playerId)

        Assertions.assertFalse(
            this.players
                .any { p: Player? -> p === player })

        Assertions.assertEquals(0, this.players.getTotalNumberOfPlayers())
        Assertions.assertEquals(0, this.players.getNumberOfConnectedPlayers())
    }

    @Test
    fun testRequestCurrentView() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()
        val two = newId()
        this.players.createOrGet(one).join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        this.players.createOrGet(two).join(
            JoinCommandDto
                .newBuilder()
                .setName("two")
                .build()
        )

        this.players.requestCurrentView(one)

        val directedCommandDto = eventReference.get()!!.getDirectedCommand()
        Assertions.assertNotNull(directedCommandDto)

        Assertions.assertTrue(directedCommandDto!!.hasCurrentView())

        val command = directedCommandDto.getCurrentView()

        Assertions.assertNotNull(command)
        Assertions.assertEquals(2, command!!.playersCount)

        val playerIds = command.playersList
            .map { obj -> obj.playerId }
            .map { EntityId.Companion.of(it) }
            .toHashSet()

        Assertions.assertTrue(playerIds.contains(one))
        Assertions.assertTrue(playerIds.contains(two))
    }

    @Test
    fun testMove() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 1025L
        player.move(0.1f, 0.1f, Math.PI.toFloat())
        player.fixedUpdate(1025L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasPlayerMoved())
        val event = eventReference.get()!!.getEvent().getPlayerMoved()

        Assertions.assertNotNull(event)
        Assertions.assertEquals(0.1f, event!!.x)
        Assertions.assertEquals(0.1f, event.y)
        Assertions.assertEquals(Math.PI.toFloat(), event.angle)
        Assertions.assertEquals((Math.PI / 4f).toFloat(), event.thrustAngle)
    }

    @Test
    fun testLimitedMove() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 1025L
        player.move(0f, 2f, null as MoveReason?)
        player.fixedUpdate(1025L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasPlayerMoved())
        val event = eventReference.get()!!.getEvent().getPlayerMoved()

        Assertions.assertNotNull(event)
        Assertions.assertEquals(0f, event!!.x, 0.0001f)
        Assertions.assertEquals(1f, event.y)
        Assertions.assertEquals(0f, event.angle)
    }

    @Test
    fun testBounceMove() {
        val eventReference = AtomicReference<Dto>(null)
        getClientEvents().registerEventDispatcher { newValue: Dto? -> eventReference.set(newValue) }

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 1025L

        player.moved(
            PlayerMovedEventDto.newBuilder()
                .setPlayerId(one.getGuid())
                .setAngle(-1f)
                .setThrustAngle(0f)
                .setTimestamp(1025)
                .setVelocity(0f)
                .setX(0.1f)
                .setY(0.1f)
                .build()
        )

        every { clock.getTime() } returns 2025L
        // from (0.1,0.1) should hit (0,0) and bounce to about (0.2,0.2)
        player.move(-0.3f, -0.3f, -1f)
        player.fixedUpdate(2025L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasPlayerMoved())
        val event = eventReference.get()!!.getEvent().getPlayerMoved()

        Assertions.assertNotNull(event)
        Assertions.assertEquals(0.2f, event!!.x, 0.0001f)
        Assertions.assertEquals(0.2f, event.y, 0.0001f)
        Assertions.assertEquals(-1f, event.angle)
    }

    @Test
    fun testOnlyAngleMove() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 75L
        player.move(0f, 0f, Math.PI.toFloat())
        player.fixedUpdate(75L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasPlayerMoved())
        val event = eventReference.get()!!.getEvent().getPlayerMoved()

        Assertions.assertNotNull(event)
        Assertions.assertEquals(0f, event!!.x)
        Assertions.assertEquals(0f, event.y)
        Assertions.assertEquals(Math.PI.toFloat(), event.angle)
    }

    @Test
    fun testNoMove() {
        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 50L

        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        player.move(0f, 0f, null as MoveReason?)

        Assertions.assertNull(eventReference.get())
    }

    @Test
    fun testMaxFire() {
        Config.setMaxBolts(2)

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()

        player.fire()
        player.fire()
        player.fire()


        Assertions.assertEquals(2, player.getActiveBoltCount())
    }

    @Test
    fun testBoltExhaustion() {
        Config.setMaxBolts(2)

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )

        every { clock.getTime() } returns 25L
        player.spawn()

        player.fire()

        Assertions.assertEquals(1, player.getActiveBoltCount())

        player.boltExhausted()

        Assertions.assertEquals(0, player.getActiveBoltCount())
    }

    @Test
    fun testFireDirection() {
        Config.setMaxBolts(2)
        Config.setBoltInertiaEnabled(false)

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )

        every { clock.getTime() } returns 25L
        player.spawn()

        every { clock.getTime() } returns 50L
        player.move(0.5f, 0.5f, Math.PI.toFloat())

        val eventReference = AtomicReference<DomainEvent?>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        player.fire()

        var event = eventReference.get()
        Assertions.assertNotNull(event)
        Assertions.assertTrue(event!!.event!!.hasPlayerFired())

        player.fired(event.event.getPlayerFired())

        event = eventReference.get()
        Assertions.assertNotNull(event)
        Assertions.assertTrue(event!!.event!!.hasBoltFired())
        Assertions.assertEquals(Math.PI.toFloat(), event.event.getBoltFired().angle, Float.Companion.MIN_VALUE)
    }

    @Test
    @Disabled
    fun testFireDirectionWithInertia() {
        Config.setMaxBolts(2)
        Config.setBoltInertiaEnabled(true)
        Config.setBoltInertiaProjectionScale(1f)
        Config.setBoltInertiaRejectionScale(1f)
        Config.setBoltInertiaNegativeProjectionScale(1f)
        Config.setPlayerMaxMove(20f)
        Config.setBoltSpeed(20f)

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )

        every { clock.getTime() } returns 25L
        player.spawn()

        every { clock.getTime() } returns 1025L
        player.move(0f, 0.5f, 0f)

        val eventReference = AtomicReference<DomainEvent?>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        player.fire()

        var event = eventReference.get()
        Assertions.assertNotNull(event)
        Assertions.assertTrue(event!!.event!!.hasPlayerFired())

        player.fired(event.event.getBoltFired())

        event = eventReference.get()
        Assertions.assertNotNull(event)
        Assertions.assertTrue(event!!.event!!.hasBoltFired())
        Assertions.assertEquals(
            Math.PI.toFloat() / 4f,
            event.event.getBoltFired().angle,
            Float.Companion.MIN_VALUE
        )
    }

    @Test
    fun testCollisionHitBullsEye() {
        Config.setUpdateFrequencyInMillis(1000L)

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )

        every { clock.getTime() } returns 25L
        player.spawn()

        every { clock.getTime() } returns 1025L
        player.move(0.5f, 0.5f, null as MoveReason?)

        val positionVector = of(
            1f,
            1f,
            (5 * Math.PI / 4f).toFloat(),
            -0.5f / cos(5 * Math.PI / 4).toFloat(),
            25L,
            PositionVector.DEFAULT_CONFIGURATION
        ).update(1025L)

        Assertions.assertTrue(player.collision(positionVector, 0.05f))
    }

    @Test
    fun testCollisionHitWithinRadius() {
        Config.setUpdateFrequencyInMillis(1000L)
        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 1025L
        player.move(0.54f, 0.54f, null as MoveReason?)
        player.fixedUpdate(1025L)

        val positionVector = of(
            0.45f,
            0.45f,
            (Math.PI / 4).toFloat(),
            0.1f / cos(Math.PI / 4).toFloat(),
            0L,
            PositionVector.DEFAULT_CONFIGURATION
        ).update(1000L)

        Assertions.assertTrue(player.collision(positionVector, 0.05f))
    }

    @Test
    fun testCollisionNoHitInsideSquare() {
        Config.setUpdateFrequencyInMillis(1000L)

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 50L
        player.move(0.4f, 0.6f, null as MoveReason?)

        val positionVector = of(
            0.45f,
            0.45f,
            (Math.PI / 4).toFloat(),
            0.1f / cos(Math.PI / 4).toFloat(),
            975L,
            PositionVector.DEFAULT_CONFIGURATION
        ).update(1025L)

        Assertions.assertFalse(player.collision(positionVector, 0.05f))
    }

    @Test
    fun testCollisionNoHitOutsideSquareX() {
        Config.setUpdateFrequencyInMillis(1000L)

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 50L
        player.move(0.2f, 0.2f, null as MoveReason?)

        val positionVector = of(
            0.45f,
            0.45f,
            (Math.PI / 4).toFloat(),
            0.1f / cos(Math.PI / 4).toFloat(),
            25L,
            PositionVector.DEFAULT_CONFIGURATION
        ).update(1025L)

        Assertions.assertFalse(player.collision(positionVector, 0.05f))
    }

    @Test
    fun testCollisionNoHitOutsideSquareY() {
        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()
        every { clock.getTime() } returns 50L
        player.move(0.5f, 0.2f, null as MoveReason?)

        val positionVector = of(
            0.45f,
            0.45f,
            (Math.PI / 4).toFloat(),
            0.1f / cos(Math.PI / 4).toFloat(),
            0L,
            PositionVector.DEFAULT_CONFIGURATION
        ).update(1000L)

        Assertions.assertFalse(player.collision(positionVector, 0.05f))
    }

    @Test
    fun testDestroyedBy() {
        val one = newId()
        val two = newId()
        val player1 = this.players.createOrGet(one)
        val player2 = this.players.createOrGet(two)

        player1.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        player2.join(
            JoinCommandDto
                .newBuilder()
                .setName("two")
                .build()
        )

        every { clock.getTime() } returns 25L
        player1.spawn()
        player2.spawn()

        every { clock.getTime() } returns 50L
        player1.move(0.5f, 0.5f, 1f)

        val domainEvents = ArrayList<DomainEvent>()
        getDomainEvents().registerEventDispatcher { e -> domainEvents.add(e) }

        player1.destroy(two, clock.getTime())

        val events = domainEvents
            .map { it.event }


        Assertions.assertEquals(1, events.size)
        Assertions.assertTrue(events[0]!!.hasPlayerDestroyed())

        verify(exactly = 1) { scoreBoard.updateScore(two, 1) }
    }

    @Test
    fun testSpawn() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val one = newId()
        val player = this.players.createOrGet(one)
        player.join(
            JoinCommandDto
                .newBuilder()
                .setName("one")
                .build()
        )
        every { clock.getTime() } returns 25L
        player.spawn()

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasPlayerSpawned())
        val playerSpawnedEvent = eventReference.get()!!.getEvent().getPlayerSpawned()
        val playerMovedEventDto = playerSpawnedEvent.location
        Assertions.assertEquals(0f, playerMovedEventDto.x)
        Assertions.assertEquals(0f, playerMovedEventDto.y)
        Assertions.assertEquals(0f, playerMovedEventDto.angle)
    }
}