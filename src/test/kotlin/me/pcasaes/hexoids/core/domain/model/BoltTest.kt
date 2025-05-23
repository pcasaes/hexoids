package me.pcasaes.hexoids.core.domain.model

import io.mockk.every
import io.mockk.mockk
import me.pcasaes.hexoids.core.domain.config.Config
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
import java.util.concurrent.atomic.AtomicReference

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

        Config.setBoltMaxDuration(10000)
        Config.setBoltSpeed(0.01F)
        Config.setBoltCollisionRadius(0.001F)
        Config.setMinMove(0.000000001F)

        every { players.iterator() } returns emptyList<Player>().iterator()
        every { players.spliterator() } returns emptyList<Player>().spliterator()

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
        Config.setBoltMaxDuration(1500)
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            0F,
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        val boltId = bolt!!.id
        Assertions.assertNotNull(bolt)

        Assertions.assertTrue(bolt.isOwnedBy(one))

        Assertions.assertEquals(1, this.bolts.getTotalNumberOfActiveBolts())


        Assertions.assertEquals(
            1,
            bolts
                .count()
        )

        Assertions.assertTrue(
            bolts
                .any { b: Bolt? -> bolt == b })

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
                .count()
        )

        Assertions.assertEquals(0, this.bolts.getTotalNumberOfActiveBolts())
    }

    @Test
    fun testMoveRight() {
        val eventReference = AtomicReference<DomainEvent?>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        Config.setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            0F,
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0.01F, bolt!!.positionVector.x)
        Assertions.assertEquals(0F, bolt.positionVector.y)
    }

    @Test
    fun testMoveRightDown() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        Config.setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            Math.PI.toFloat() / 4F,
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0.0070710676F, bolt!!.positionVector.x)
        Assertions.assertEquals(0.0070710676F, bolt.positionVector.y)
    }

    @Test
    fun testMoveDown() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        Config.setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0F,
            0F,
            Math.PI.toFloat() / 2F,
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0F, bolt!!.positionVector.x)
        Assertions.assertEquals(0.01F, bolt.positionVector.y)
    }

    @Test
    fun testMoveLeftDown() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        Config.setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            0F,
            (3 * Math.PI / 4F).toFloat(),
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(1F - 0.0070710676F, bolt!!.positionVector.x)
        Assertions.assertEquals(0.0070710676F, bolt.positionVector.y)
    }

    @Test
    fun testMoveLeft() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        Config.setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            0F,
            Math.PI.toFloat(),
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(0.99F, bolt!!.positionVector.x)
        Assertions.assertEquals(0F, bolt.positionVector.y)
    }

    @Test
    fun testMoveLefUp() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        Config.setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            1F,
            (5 * Math.PI / 4F).toFloat(),
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(1F - 0.0070710676F, bolt!!.positionVector.x)
        Assertions.assertEquals(1F - 0.0070710676F, bolt.positionVector.y)
    }

    @Test
    fun testMoveUp() {
        val eventReference = AtomicReference<DomainEvent>(null)
        getDomainEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        Config.setBoltMaxDuration(1500)
        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            1F,
            1F,
            (3 * Math.PI / 2F).toFloat(),
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)

        every { clock.getTime() } returns 1000L

        bolts.fixedUpdate(clock.getTime())

        Assertions.assertEquals(1F, bolt!!.positionVector.x)
        Assertions.assertEquals(0.99F, bolt.positionVector.y)
    }

    @Test
    fun testCollisionHit() {
        Config.setBoltMaxDuration(1500)

        val player = mockk<Player>(relaxed = true)

        val playersList = mutableListOf(player)

        every { players.iterator() } returns playersList.iterator()
        every { players.spliterator() } returns playersList.spliterator()

        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0.5F,
            0.5F,
            0F,
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)
        val boltId = bolt!!.id

        every { clock.getTime() } returns 1000L

        every { player.collision(bolt.positionVector, Config.getBoltCollisionRadius()) } returns true


        val domainEvents = ArrayList<DomainEvent>()
        getDomainEvents().registerEventDispatcher { e -> domainEvents.add(e) }

        bolts.fixedUpdate(clock.getTime())

        val events = domainEvents
            .map { d -> d.event }


        val boltExhaustedEventDto = events
            .filter { obj -> obj!!.hasBoltExhausted() }
            .map { obj -> obj!!.getBoltExhausted() }
            .firstOrNull()

        Assertions.assertNotNull(boltExhaustedEventDto)
        Assertions.assertArrayEquals(
            boltId.getGuid().guid.toByteArray(),
            boltExhaustedEventDto!!.boltId.guid.toByteArray()
        )
    }

    @Test
    fun testCollisionMiss() {
        Config.setBoltMaxDuration(1500)

        val player = mockk<Player>(relaxed = true)

        val playersList = mutableListOf(player)

        every { players.iterator() } returns playersList.iterator()
        every { players.spliterator() } returns playersList.spliterator()


        val one = newId()
        val bolt = bolts.fired(
            players,
            newId(),
            one,
            0.5F,
            0.5F,
            0F,
            Config.getBoltSpeed(),
            clock.getTime(),
            Config.getBoltMaxDuration()
        )
        Assertions.assertNotNull(bolt)


        every { clock.getTime() } returns 1000L
        every { player.collision(bolt!!.positionVector, Config.getBoltCollisionRadius()) } returns false

        val events = ArrayList<DomainEvent>()
        getDomainEvents().registerEventDispatcher { e -> events.add(e) }

        bolts.fixedUpdate(clock.getTime())


        Assertions.assertEquals(
            0, events
                .map { d -> d.event }.count { obj -> obj!!.hasBoltExhausted() })
    }
}