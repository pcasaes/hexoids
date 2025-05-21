package me.pcasaes.hexoids.core.domain.model

import io.mockk.every
import io.mockk.mockk
import me.pcasaes.hexoids.core.domain.model.Clock.Companion.create
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.newId
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getDomainEvents
import me.pcasaes.hexoids.core.domain.model.ScoreBoard.Companion.create
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pcasaes.hexoids.proto.Dto
import java.util.concurrent.atomic.AtomicReference


class ScoreBoardTest {

    private val game = mockk<Game>(relaxed = true)

    private lateinit var scoreBoard: ScoreBoard

    @BeforeEach
    fun setup() {
        getClientEvents().registerEventDispatcher(null)

        GameTopic.setGame(game)

        scoreBoard = create(create())

        every { game.getScoreBoard() } returns scoreBoard

        getDomainEvents().registerEventDispatcher { domainEvent ->
            GameTopic.valueOf(domainEvent.topic!!).consume(
                domainEvent
            )
        }
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

        scoreBoard.fixedUpdate(1000L)

        scoreBoard.resetScore(one)


        scoreBoard.fixedUpdate(2000L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
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

        val ids = ArrayList<EntityId>(ScoreBoard.Implementation.SCORE_BOARD_SIZE)

        repeat(ScoreBoard.Implementation.SCORE_BOARD_SIZE) { i ->
            ids.add(newId())
        }

        for (i in 0..<ScoreBoard.Implementation.SCORE_BOARD_SIZE) {
            scoreBoard.updateScore(ids[i], ScoreBoard.Implementation.SCORE_BOARD_SIZE - i)
        }

        scoreBoard.fixedUpdate(1000L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
        val event = eventReference.get()!!.getEvent().getScoreBoardUpdated()
        Assertions.assertNotNull(event)

        Assertions.assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE, event!!.scoresCount)

        for (i in 0..<ScoreBoard.Implementation.SCORE_BOARD_SIZE) {
            Assertions.assertEquals(ids[i].getGuid(), event.scoresList[i].playerId)
            Assertions.assertEquals(
                ScoreBoard.Implementation.SCORE_BOARD_SIZE - i,
                event.scoresList[i].score
            )
        }
    }

    @Test
    fun testSimplePastFull() {
        val eventReference = AtomicReference<Dto?>(null)
        getClientEvents().registerEventDispatcher { newValue -> eventReference.set(newValue) }

        val ids = ArrayList<EntityId>(ScoreBoard.Implementation.SCORE_BOARD_SIZE)

        repeat(ScoreBoard.Implementation.SCORE_BOARD_SIZE) {
            ids.add(newId())
        }

        for (i in 0..<ScoreBoard.Implementation.SCORE_BOARD_SIZE) {
            scoreBoard.updateScore(ids[i], ScoreBoard.Implementation.SCORE_BOARD_SIZE - i)
        }

        scoreBoard.fixedUpdate(1000L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
        var event = eventReference.get()!!.getEvent().getScoreBoardUpdated()
        Assertions.assertNotNull(event)

        val a = newId()
        val b = newId()
        val c = newId()
        scoreBoard.updateScore(a, 100)
        scoreBoard.updateScore(b, 3)
        scoreBoard.updateScore(c, -1)

        scoreBoard.fixedUpdate(2000L)

        Assertions.assertTrue(eventReference.get()!!.hasEvent())
        Assertions.assertTrue(eventReference.get()!!.getEvent().hasScoreBoardUpdated())
        event = eventReference.get()!!.getEvent().getScoreBoardUpdated()
        Assertions.assertNotNull(event)

        Assertions.assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE, event.scoresCount)

        Assertions.assertEquals(a.getGuid(), event.scoresList[0].playerId)
        Assertions.assertEquals(100, event.scoresList[0].score)

        Assertions.assertEquals(
            b.getGuid(),
            event.scoresList[ScoreBoard.Implementation.SCORE_BOARD_SIZE - 1].playerId
        )
        Assertions.assertEquals(3, event.scoresList[ScoreBoard.Implementation.SCORE_BOARD_SIZE - 1].score)

        for (i in 0..<ScoreBoard.Implementation.SCORE_BOARD_SIZE - 2) {
            Assertions.assertEquals(ids[i].getGuid(), event.scoresList[i + 1].playerId)
            Assertions.assertEquals(
                ScoreBoard.Implementation.SCORE_BOARD_SIZE - i,
                event.scoresList[i + 1].score
            )
        }
    }
}