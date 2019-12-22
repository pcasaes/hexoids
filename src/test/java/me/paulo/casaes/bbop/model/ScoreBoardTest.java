package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.Dto;
import me.paulo.casaes.bbop.dto.ScoreBoardUpdatedEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static me.paulo.casaes.bbop.model.ScoreBoard.Implementation.SCORE_BOARD_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScoreBoardTest {

    @BeforeEach
    void setup() {
        GameEvents.getClientEvents().setConsumer(null);

        ScoreBoard.Factory.get().reset();

    }

    @Test
    void testNotEnoughTime() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        ScoreBoard.Factory.get().fixedUpdate(500L);

        assertNull(eventReference.get());
    }

    @Test
    void testEmptyLeaderBoard() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        ScoreBoard.Factory.get().fixedUpdate(1000L);

        assertNull(eventReference.get());
    }

    @Test
    void testSimpleReset() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        ScoreBoard.Factory.get().updateScore("1", 100);

        ScoreBoard.Factory.get().fixedUpdate(1000L);

        ScoreBoard.Factory.get().resetScore("1");


        ScoreBoard.Factory.get().fixedUpdate(2000L);

        ScoreBoardUpdatedEventDto event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        assertEquals(1, event.getScores().size());

        assertEquals("1", event.getScores().get(0).getPlayerId());
        assertEquals(0, event.getScores().get(0).getScore());
    }

    @Test
    void testSimpleFull() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        for (int i = 0; i < SCORE_BOARD_SIZE; i++) {
            ScoreBoard.Factory.get().updateScore(String.valueOf(i), SCORE_BOARD_SIZE - i);
        }

        ScoreBoard.Factory.get().fixedUpdate(1000L);

        ScoreBoardUpdatedEventDto event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        assertEquals(SCORE_BOARD_SIZE, event.getScores().size());

        for (int i = 0; i < SCORE_BOARD_SIZE; i++) {
            assertEquals(String.valueOf(i), event.getScores().get(i).getPlayerId());
            assertEquals(SCORE_BOARD_SIZE - i, event.getScores().get(i).getScore());
        }

    }

    @Test
    void testSimplePastFull() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        for (int i = 0; i < SCORE_BOARD_SIZE; i++) {
            ScoreBoard.Factory.get().updateScore(String.valueOf(i), SCORE_BOARD_SIZE - i);
        }

        ScoreBoard.Factory.get().fixedUpdate(1000L);

        ScoreBoardUpdatedEventDto event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        ScoreBoard.Factory.get().updateScore("a", 100);
        ScoreBoard.Factory.get().updateScore("b", 3);
        ScoreBoard.Factory.get().updateScore("c", -1);

        ScoreBoard.Factory.get().fixedUpdate(2000L);

        event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        assertEquals(SCORE_BOARD_SIZE, event.getScores().size());

        assertEquals("a", event.getScores().get(0).getPlayerId());
        assertEquals(100, event.getScores().get(0).getScore());

        assertEquals("b", event.getScores().get(SCORE_BOARD_SIZE - 1).getPlayerId());
        assertEquals(3, event.getScores().get(SCORE_BOARD_SIZE - 1).getScore());

        for (int i = 0; i < SCORE_BOARD_SIZE - 2; i++) {
            assertEquals(String.valueOf(i), event.getScores().get(i + 1).getPlayerId());
            assertEquals(SCORE_BOARD_SIZE - i, event.getScores().get(i + 1).getScore());
        }

    }
}