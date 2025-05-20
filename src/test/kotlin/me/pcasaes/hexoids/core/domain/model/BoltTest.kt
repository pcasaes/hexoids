package me.pcasaes.hexoids.core.domain.model

import io.mockk.every
import io.mockk.mockk
import me.pcasaes.hexoids.core.domain.config.Config.Companion.get
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndex
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndexFactory
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory
import me.pcasaes.hexoids.core.domain.model.Bolts.Companion.create
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.newId
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getDomainEvents
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pcasaes.hexoids.proto.Event
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.stream.Collectors

class BoltTest {

    private lateinit var clock: Clock

    private lateinit var players: Players

    private lateinit var bolts: Bolts

    @BeforeEach
    fun setup() {
        clock = mockk(relaxed = true)
        players = mockk(relaxed = true)

        getClientEvents().registerEventDispatcher(null)
        getDomainEvents().registerEventDispatcher(null)

        bolts = create()

        Assertions.assertEquals(0, this.bolts.getTotalNumberOfActiveBolts())

        every { clock.getTime() } returns 0L

        get().setBoltMaxDuration(10000)
        get().setBoltSpeed(0.01F)
        get().setBoltCollisionRadius(0.001F)
        get().setMinMove(0.000000001F)

        every { players.iterator() } returns emptyList<Player>().iterator()
        every { players.spliterator() } returns emptyList<Player>().spliterator()
        every { players.stream() } returns emptyList<Player>().stream()

        PlayerSpatialIndexFactory
            .factory()
            .setPlayerSpatialIndex { x1: Float, y1: Float, x2: Float, y2: Float, distance: Float -> players }

        BarrierSpatialIndexFactory
            .factory()
            .setBarrierSpatialIndex(object : BarrierSpatialIndex {
                override fun search(x1: Float, y1: Float, x2: Float, y2: Float, distance: Float): Iterable<Barrier> {
                    return mutableListOf()
                }

                override fun update(barriers: Iterable<Barrier>) {
                }
            })

        every { players.getSpatialIndex() } returns PlayerSpatialIndexFactory.factory().get()
    }

    @Test
    fun testFireMoveAndExhaust() {
        val events = ArrayList<DomainEvent>()
        getDomainEvents().registerEventDispatcher { e -> events.add(e) }

        val one = newId()
        get().setBoltMaxDuration(1500)
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            0F,
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        val boltId = bolt!!.id
        Assertions.assertNotNull(bolt)

        Assertions.assertTrue(bolt.isOwnedBy(one))

        Assertions.assertEquals(1, this.bolts.getTotalNumberOfActiveBolts())


        Assertions.assertEquals(
            1,
            bolts
                .stream()
                .count()
        )

        Assertions.assertTrue(
            bolts
                .stream()
                .anyMatch { b: Bolt? -> bolt == b })

        Assertions.assertFalse(bolt.isExhausted)
        Assertions.assertTrue(bolt.isActive)

        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertFalse(bolt.isExhausted)
        Assertions.assertTrue(bolt.isActive)

        every { clock.getTime() } returns 2000L

        events.clear()
        bolts.fixedUpdate(clock.getTime())
        Assertions.assertEquals(1, events.size)
        val domainEvent = events[0]
        Assertions.assertNotNull(domainEvent)
        Assertions.assertNotNull(domainEvent.event)

        Assertions.assertTrue(domainEvent.event!!.hasBoltExhausted())
        val exhaustedEvent = domainEvent.event.getBoltExhausted()

        Assertions.assertNotNull(exhaustedEvent)
        Assertions.assertArrayEquals(
            boltId.getGuid().guid.toByteArray(),
            exhaustedEvent!!.boltId.guid.toByteArray()
        )

        Assertions.assertTrue(bolt.isExhausted)
        Assertions.assertFalse(bolt.isActive)

        events.clear()

        Assertions.assertEquals(
            0, bolts
                .stream()
                .count()
        )

        Assertions.assertEquals(0, this.bolts.getTotalNumberOfActiveBolts())
    }

    @Test
    fun testMoveRight() {
        val eventReference = AtomicReference<DomainEvent?>(null)
        getDomainEvents().registerEventDispatcher(Consumer { newValue -> eventReference.set(newValue) })

        get().setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            0F,
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0.01F, bolt.positionVector.getX())
        Assertions.assertEquals(0F, bolt.positionVector.getY())
    }

    @Test
    fun testMoveRightDown() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher(Consumer { newValue -> eventReference.set(newValue) })

        get().setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            Math.PI.toFloat() / 4F,
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0.0070710676F, bolt.positionVector.getX())
        Assertions.assertEquals(0.0070710676F, bolt.positionVector.getY())
    }

    @Test
    fun testMoveDown() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher(Consumer { newValue -> eventReference.set(newValue) })

        get().setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            Math.PI.toFloat() / 2F,
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0F, bolt!!.positionVector.getX())
        Assertions.assertEquals(0.01F, bolt.positionVector.getY())
    }

    @Test
    fun testMoveLeftDown() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher(Consumer { newValue -> eventReference.set(newValue) })

        get().setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            0F,
            (3 * Math.PI / 4F).toFloat(),
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(1F - 0.0070710676F, bolt!!.positionVector.getX())
        Assertions.assertEquals(0.0070710676F, bolt.positionVector.getY())
    }

    @Test
    fun testMoveLeft() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher(Consumer { newValue -> eventReference.set(newValue) })

        get().setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            0F,
            Math.PI.toFloat(),
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0.99F, bolt!!.positionVector.getX())
        Assertions.assertEquals(0F, bolt.positionVector.getY())
    }

    @Test
    fun testMoveLefUp() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher(Consumer { newValue -> eventReference.set(newValue) })

        get().setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            1F,
            (5 * Math.PI / 4F).toFloat(),
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(1F - 0.0070710676F, bolt!!.positionVector.getX())
        Assertions.assertEquals(1F - 0.0070710676F, bolt.positionVector.getY())
    }

    @Test
    fun testMoveUp() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher(Consumer { newValue -> eventReference.set(newValue) })

        get().setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            1F,
            (3 * Math.PI / 2F).toFloat(),
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)

        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(1F, bolt!!.positionVector.getX())
        Assertions.assertEquals(0.99F, bolt.positionVector.getY())
    }

    @Test
    fun testCollisionHit() {
        get().setBoltMaxDuration(1500)

        val player = mockk<Player>(relaxed = true)

        val playersList = mutableListOf(player)

        every { players.iterator() } returns playersList.iterator()
        every { players.spliterator() } returns playersList.spliterator()
        every { players.stream() } returns playersList.stream()

        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0.5F,
            0.5F,
            0F,
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)
        val boltId = bolt!!.id

        every { clock.getTime() } returns 1000L

        every { player.collision(bolt.positionVector, get().getBoltCollisionRadius()) } returns true


        val domainEvents = ArrayList<DomainEvent>()
        getDomainEvents().registerEventDispatcher { e -> domainEvents.add(e) }

        bolts.fixedUpdate(clock.getTime())

        val events = domainEvents
            .stream()
            .map { d -> d.event }
            .collect(Collectors.toList())


        val boltExhaustedEventDto = events
            .stream()
            .filter { obj -> obj!!.hasBoltExhausted() }
            .map { obj -> obj!!.getBoltExhausted() }
            .findFirst().orElse(null)

        Assertions.assertNotNull(boltExhaustedEventDto)
        Assertions.assertArrayEquals(
            boltId.getGuid().guid.toByteArray(),
            boltExhaustedEventDto!!.boltId.getGuid().toByteArray()
        )
    }

    @Test
    fun testCollisionMiss() {
        get().setBoltMaxDuration(1500)

        val player = mockk<Player>(relaxed = true)

        val playersList = mutableListOf(player)

        every { players.iterator() } returns playersList.iterator()
        every { players.spliterator() } returns playersList.spliterator()
        every { players.stream() } returns playersList.stream()


        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0.5F,
            0.5F,
            0F,
            get().getBoltSpeed(),
            clock.getTime(),
            get().getBoltMaxDuration()
        ).orElse(null)
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L
        every { player.collision(bolt.positionVector, get().getBoltCollisionRadius()) } returns false

        val events = ArrayList<DomainEvent>()
        getDomainEvents().registerEventDispatcher(Consumer { e -> events.add(e) })

        bolts.fixedUpdate(clock.getTime())


        Assertions.assertEquals(
            0, events
                .stream()
                .map<Event?> { d: DomainEvent? -> d!!.event }
                .filter { obj: Event? -> obj!!.hasBoltExhausted() }
                .count())
    }
}