package me.pcasaes.hexoids.infrastructure.vertx.cluster

import io.mockk.every
import io.mockk.mockk
import io.vertx.core.Vertx
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueueFactory
import me.pcasaes.hexoids.core.domain.model.Clock
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.newId
import me.pcasaes.hexoids.core.domain.model.Game
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getDomainEvents
import me.pcasaes.hexoids.core.domain.model.GameTopic
import me.pcasaes.hexoids.core.domain.model.ScoreBoard
import me.pcasaes.hexoids.core.domain.model.ScoreBoard.Companion.create
import me.pcasaes.hexoids.core.domain.model.ScoreBoard.Implementation.Companion.SCORE_BOARD_SIZE
import me.pcasaes.hexoids.core.domain.repostiory.ScoreBoardRepositoryFactory
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pcasaes.hexoids.proto.Dto
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ClusteredScoreBoardRepositoryTest {

    private val game = mockk<Game>(relaxed = true)

    private val clock = mockk<Clock>()

    private lateinit var scoreBoard: ScoreBoard

    private lateinit var vertx: Vertx

    private val domainEventsFired = AtomicInteger(0)

    @BeforeEach
    fun setup() {
        vertx = Vertx.vertx()

        getClientEvents().registerEventDispatcher(null)
        GameQueueFactory.register {
            it.run()
        }

        ScoreBoardRepositoryFactory.register(
            ClusteredScoreBoardRepository(io.vertx.mutiny.core.Vertx(vertx))
        )


        GameTopic.setGame(game)

        every { clock.getTime() } returns 0L

        scoreBoard = create(clock)

        every { game.getScoreBoard() } returns scoreBoard

        domainEventsFired.set(0)

        getDomainEvents().registerEventDispatcher { domainEvent ->
            GameTopic.valueOf(domainEvent.topic!!).consume(
                domainEvent
            )
            domainEventsFired.incrementAndGet()
        }
    }

    @AfterEach
    fun teardown() {
        vertx.close().toCompletionStage().toCompletableFuture().get()
    }

    @Test
    fun testNotEnoughTime() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        scoreBoard.fixedUpdate(500L)

        Assertions.assertNull(eventReference.get())
    }

    @Test
    fun testEmptyLeaderBoard() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        scoreBoard.fixedUpdate(1000L)

        Assertions.assertNull(eventReference.get())
    }

    @Test
    fun testSimpleReset() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }


        val one = newId()
        scoreBoard.updateScore(one, 100)

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                assertTrue(domainEventsFired.get() >= 1)
            }


        scoreBoard.fixedUpdate(1000L)

        scoreBoard.resetScore(one)

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                assertTrue(domainEventsFired.get() >= 2)
            }


        scoreBoard.fixedUpdate(2000L)

        assertTrue(eventReference.get()!!.hasEvent())
        assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
        val event = eventReference.get()!!.getEvent().getScoreBoardUpdated()
        Assertions.assertNotNull(event)

        Assertions.assertEquals(1, event!!.scoresCount)

        Assertions.assertEquals(one.getGuid(), event.scoresList[0].playerId)
        Assertions.assertEquals(0, event.scoresList[0].score)
    }

    @Test
    fun testSimpleFull() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val ids = ArrayList<EntityId>(SCORE_BOARD_SIZE)

        repeat(SCORE_BOARD_SIZE) { i ->
            ids.add(newId())
        }

        for (i in 0..<SCORE_BOARD_SIZE) {
            scoreBoard.updateScore(ids[i], SCORE_BOARD_SIZE - i)
        }

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                assertTrue(domainEventsFired.get() >= SCORE_BOARD_SIZE)
            }

        scoreBoard.fixedUpdate(1000L)

        assertTrue(eventReference.get()!!.hasEvent())
        assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
        val event = eventReference.get()!!.getEvent().getScoreBoardUpdated()
        Assertions.assertNotNull(event)

        Assertions.assertEquals(SCORE_BOARD_SIZE, event!!.scoresCount)

        for (i in 0..<SCORE_BOARD_SIZE) {
            Assertions.assertEquals(ids[i].getGuid(), event.scoresList[i].playerId)
            Assertions.assertEquals(
                SCORE_BOARD_SIZE - i,
                event.scoresList[i].score
            )
        }
    }

    @Test
    fun testSimplePastFull() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val ids = ArrayList<EntityId>(SCORE_BOARD_SIZE)

        repeat(SCORE_BOARD_SIZE) {
            ids.add(newId())
        }

        every { clock.getTime() } returns 1000L

        for (i in 0..<SCORE_BOARD_SIZE) {
            val score = SCORE_BOARD_SIZE - i
            scoreBoard.updateScore(ids[i], score)
        }

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                assertTrue(domainEventsFired.get() >= SCORE_BOARD_SIZE)
            }


        scoreBoard.fixedUpdate(1000L)

        assertTrue(eventReference.get()!!.hasEvent())
        assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
        var event = eventReference.get()!!.getEvent().getScoreBoardUpdated()
        Assertions.assertNotNull(event)

        every { clock.getTime() } returns 2000L

        val a = newId()
        val b = newId()
        val c = newId()
        scoreBoard.updateScore(a, 100)
        scoreBoard.updateScore(b, 3)
        scoreBoard.updateScore(c, -1)

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                assertTrue(domainEventsFired.get() >= SCORE_BOARD_SIZE + 3)
            }

        scoreBoard.fixedUpdate(2000L)

        assertTrue(eventReference.get()!!.hasEvent())
        assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
        event = eventReference.get()!!.getEvent().getScoreBoardUpdated()
        Assertions.assertNotNull(event)

        Assertions.assertEquals(SCORE_BOARD_SIZE, event.scoresCount)

        Assertions.assertEquals(a.getGuid(), event.scoresList[0].playerId)
        Assertions.assertEquals(100, event.scoresList[0].score)

        Assertions.assertEquals(
            b.getGuid(),
            event.scoresList[SCORE_BOARD_SIZE - 1].playerId
        )
        Assertions.assertEquals(3, event.scoresList[SCORE_BOARD_SIZE - 1].score)

        for (i in 0..<SCORE_BOARD_SIZE - 2) {
            Assertions.assertEquals(ids[i].getGuid(), event.scoresList[i + 1].playerId)
            Assertions.assertEquals(
                SCORE_BOARD_SIZE - i,
                event.scoresList[i + 1].score
            )
        }
    }
}