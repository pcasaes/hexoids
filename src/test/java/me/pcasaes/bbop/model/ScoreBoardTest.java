package me.pcasaes.bbop.model;

import me.pcasaes.bbop.dto.Dto;
import me.pcasaes.bbop.dto.ScoreBoardUpdatedEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

class ScoreBoardTest {

    @Mock
    private Game game;

    private ScoreBoard scoreBoard;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        GameEvents.getClientEvents().setConsumer(null);

        Topics.setGame(game);

        scoreBoard = ScoreBoard.create(Clock.create());
        doReturn(scoreBoard).when(game).getScoreBoard();

        GameEvents.getDomainEvents().setConsumer(domainEvent -> {
            Topics.valueOf(domainEvent.getTopic()).consume(domainEvent);
        });
    }

    @Test
    void testNotEnoughTime() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        scoreBoard.fixedUpdate(500L);

        assertNull(eventReference.get());
    }

    @Test
    void testEmptyLeaderBoard() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        scoreBoard.fixedUpdate(1000L);

        assertNull(eventReference.get());
    }

    @Test
    void testSimpleReset() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);


        UUID one = UUID.randomUUID();
        scoreBoard.updateScore(one, 100);

        scoreBoard.fixedUpdate(1000L);

        scoreBoard.resetScore(one);


        scoreBoard.fixedUpdate(2000L);

        ScoreBoardUpdatedEventDto event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        assertEquals(1, event.getScores().size());

        assertEquals(one.toString(), event.getScores().get(0).getPlayerId());
        assertEquals(0, event.getScores().get(0).getScore());
    }

    @Test
    void testSimpleFull() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        List<UUID> ids = new ArrayList<>(ScoreBoard.Implementation.SCORE_BOARD_SIZE);

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            ids.add(UUID.randomUUID());
        }

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            scoreBoard.updateScore(ids.get(i), ScoreBoard.Implementation.SCORE_BOARD_SIZE - i);
        }

        scoreBoard.fixedUpdate(1000L);

        ScoreBoardUpdatedEventDto event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE, event.getScores().size());

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            assertEquals(ids.get(i).toString(), event.getScores().get(i).getPlayerId());
            assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE - i, event.getScores().get(i).getScore());
        }

    }

    @Test
    void testSimplePastFull() {
        AtomicReference<Dto> eventReference = new AtomicReference<Dto>(null);
        GameEvents.getClientEvents().setConsumer(eventReference::set);

        List<UUID> ids = new ArrayList<>(ScoreBoard.Implementation.SCORE_BOARD_SIZE);

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            ids.add(UUID.randomUUID());
        }

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE; i++) {
            scoreBoard.updateScore(ids.get(i), ScoreBoard.Implementation.SCORE_BOARD_SIZE - i);
        }

        scoreBoard.fixedUpdate(1000L);

        ScoreBoardUpdatedEventDto event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        scoreBoard.updateScore(a, 100);
        scoreBoard.updateScore(b, 3);
        scoreBoard.updateScore(c, -1);

        scoreBoard.fixedUpdate(2000L);

        event = (ScoreBoardUpdatedEventDto) eventReference.get();
        assertNotNull(event);

        assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE, event.getScores().size());

        assertEquals(a.toString(), event.getScores().get(0).getPlayerId());
        assertEquals(100, event.getScores().get(0).getScore());

        assertEquals(b.toString(), event.getScores().get(ScoreBoard.Implementation.SCORE_BOARD_SIZE - 1).getPlayerId());
        assertEquals(3, event.getScores().get(ScoreBoard.Implementation.SCORE_BOARD_SIZE - 1).getScore());

        for (int i = 0; i < ScoreBoard.Implementation.SCORE_BOARD_SIZE - 2; i++) {
            assertEquals(ids.get(i).toString(), event.getScores().get(i + 1).getPlayerId());
            assertEquals(ScoreBoard.Implementation.SCORE_BOARD_SIZE - i, event.getScores().get(i + 1).getScore());
        }

    }
}