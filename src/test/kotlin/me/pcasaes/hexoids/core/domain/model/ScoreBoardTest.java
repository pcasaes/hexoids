package me.pcasaes.hexoids.core.domain.model;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.ScoreBoardUpdatedEventDto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

class ScoreBoardTest {

    @Mock
    private Game game;

    private ScoreBoard scoreBoard;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        GameEvents.getClientEvents().registerEventDispatcher(null);

        GameTopic.setGame(game);

        scoreBoard = ScoreBoard.create(Clock.create());
        doReturn(scoreBoard).when(game).getScoreBoard();

        GameEvents.getDomainEvents().registerEventDispatcher(domainEvent ->
                GameTopic.valueOf(domainEvent.topic).consume(domainEvent)
        );
    }

    @Test
    void testNotEnoughTime() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().registerEventDispatcher(eventReference::set);

        scoreBoard.fixedUpdate(500L);

        assertNull(eventReference.get());
    }

    @Test
    void testEmptyLeaderBoard() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().registerEventDispatcher(eventReference::set);

        scoreBoard.fixedUpdate(1000L);

        assertNull(eventReference.get());
    }

    @Test
    void testSimpleReset() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().registerEventDispatcher(eventReference::set);


        EntityId one = EntityId.newId();
        scoreBoard.updateScore(one, 100);

        scoreBoard.fixedUpdate(1000L);

        scoreBoard.resetScore(one);


        scoreBoard.fixedUpdate(2000L);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasScoreBoardUpdated());
        ScoreBoardUpdatedEventDto event = eventReference.get().getEvent().getScoreBoardUpdated();
        assertNotNull(event);

        assertEquals(1, event.getScoresCount());

        assertEquals(one.getGuid(), event.getScoresList().get(0).getPlayerId());
        assertEquals(0, event.getScoresList().get(0).getScore());
    }

    @Test
    void testSimpleFull() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().registerEventDispatcher(eventReference::set);

        List<EntityId> ids = new ArrayList<>(ScoreBoard.Implementation.SCORE_BOARD_SIZE);

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            ids.add(EntityId.newId());
        }

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            scoreBoard.updateScore(ids.get(i), ScoreBoard.Implementation.SCORE_BOARD_SIZE - i);
        }

        scoreBoard.fixedUpdate(1000L);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasScoreBoardUpdated());
        ScoreBoardUpdatedEventDto event = eventReference.get().getEvent().getScoreBoardUpdated();
        assertNotNull(event);

        assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE, event.getScoresCount());

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            assertEquals(ids.get(i).getGuid(), event.getScoresList().get(i).getPlayerId());
            assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE - i, event.getScoresList().get(i).getScore());
        }

    }

    @Test
    void testSimplePastFull() {
        AtomicReference<Dto> eventReference = new AtomicReference<>(null);
        GameEvents.getClientEvents().registerEventDispatcher(eventReference::set);

        List<EntityId> ids = new ArrayList<>(ScoreBoard.Implementation.SCORE_BOARD_SIZE);

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            ids.add(EntityId.newId());
        }

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            scoreBoard.updateScore(ids.get(i), ScoreBoard.Implementation.SCORE_BOARD_SIZE - i);
        }

        scoreBoard.fixedUpdate(1000L);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasScoreBoardUpdated());
        ScoreBoardUpdatedEventDto event = eventReference.get().getEvent().getScoreBoardUpdated();
        assertNotNull(event);

        EntityId a = EntityId.newId();
        EntityId b = EntityId.newId();
        EntityId c = EntityId.newId();
        scoreBoard.updateScore(a, 100);
        scoreBoard.updateScore(b, 3);
        scoreBoard.updateScore(c, -1);

        scoreBoard.fixedUpdate(2000L);

        assertTrue(eventReference.get().hasEvent());
        assertTrue(eventReference.get().getEvent().hasScoreBoardUpdated());
        event = eventReference.get().getEvent().getScoreBoardUpdated();
        assertNotNull(event);

        assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE, event.getScoresCount());

        assertEquals(a.getGuid(), event.getScoresList().get(0).getPlayerId());
        assertEquals(100, event.getScoresList().get(0).getScore());

        assertEquals(b.getGuid(), event.getScoresList().get(ScoreBoard.Implementation.SCORE_BOARD_SIZE - 1).getPlayerId());
        assertEquals(3, event.getScoresList().get(ScoreBoard.Implementation.SCORE_BOARD_SIZE - 1).getScore());

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE - 2; i++) {
            assertEquals(ids.get(i).getGuid(), event.getScoresList().get(i + 1).getPlayerId());
            assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE - i, event.getScoresList().get(i + 1).getScore());
        }

    }
}