package me.paulo.casaes.bbop.model;

public class Game {

    private static final Game GAME = new Game(Bolts.get(), ScoreBoard.get());

    private final Bolts bolts;

    private final ScoreBoard scoreBoard;

    private Game(Bolts bolts, ScoreBoard scoreBoard) {
        this.bolts = bolts;
        this.scoreBoard = scoreBoard;
    }

    public static Game get() {
        return GAME;
    }

    public void fixedUpdate(long timestamp) {
        bolts.fixedUpdate(timestamp);
        scoreBoard.fixedUpdate(timestamp);
    }
}
