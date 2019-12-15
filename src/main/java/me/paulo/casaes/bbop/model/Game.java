package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.EventDto;

import java.util.List;

public class Game {

    private static final Game GAME = new Game();

    private Game() {
    }

    public static Game get() {
        return GAME;
    }

    public List<EventDto> fixedUpdate(long timestamp) {
        return Bolt.update(timestamp);
    }
}
