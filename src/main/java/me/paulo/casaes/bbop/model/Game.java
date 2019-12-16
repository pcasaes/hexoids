package me.paulo.casaes.bbop.model;

public class Game {

    private static final Game GAME = new Game();

    private Game() {
    }

    public static Game get() {
        return GAME;
    }

    public void fixedUpdate(long timestamp) {
        Bolt.fixedUpdate(timestamp);
    }
}
