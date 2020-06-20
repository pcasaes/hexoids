package me.pcasaes.hexoids.core.domain.model;

/**
 * The Game object is a singleton access through {@link Game#get()} that is
 * the root object of the game composition.
 */
public interface Game {

    /**
     * Updates the game model to the specified timestamp.
     * <p>
     * Will move players, bolts and check for hits and score updates.
     *
     * @param timestamp time to update the game model. Should always increase.
     */
    void fixedUpdate(long timestamp);

    /**
     * Returns the players collection singleton.
     *
     * @return Players collection
     */
    Players getPlayers();

    /**
     * Returns the game's clock.
     *
     * @return Game clock
     */
    Clock getClock();

    /**
     * Returns the bolts collection singleton.
     *
     * @return Bolts collection
     */
    Bolts getBolts();

    /**
     * Returns the score board singleton.
     *
     * @return Scoreboard
     */
    ScoreBoard getScoreBoard();


    /**
     * Returns the game's singleton.
     * @return
     */
    static Game get() {
        return Implementation.INSTANCE;
    }

    class Implementation implements Game {

        private static final Game INSTANCE;

        static {
            Clock clock = Clock.create();
            Bolts bolts = Bolts.create();
            ScoreBoard scoreBoard = ScoreBoard.create(clock);
            Players players = Players.create(bolts, clock, scoreBoard);

            INSTANCE = new Implementation(players, clock, bolts, scoreBoard);
        }


        private final Players players;

        private final Clock clock;

        private final Bolts bolts;

        private final ScoreBoard scoreBoard;


        private Implementation(Players players, Clock clock, Bolts bolts, ScoreBoard scoreBoard) {
            this.players = players;
            this.clock = clock;
            this.bolts = bolts;
            this.scoreBoard = scoreBoard;

            GameTopic.setGame(this);
        }


        @Override
        public void fixedUpdate(long timestamp) {
            players.fixedUpdate(timestamp);
            bolts.fixedUpdate(timestamp);
            scoreBoard.fixedUpdate(timestamp);
        }

        @Override
        public Players getPlayers() {
            return players;
        }

        @Override
        public Clock getClock() {
            return clock;
        }

        @Override
        public Bolts getBolts() {
            return bolts;
        }

        @Override
        public ScoreBoard getScoreBoard() {
            return scoreBoard;
        }
    }
}
