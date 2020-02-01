package me.pcasaes.hexoids.model;

public interface Game {

    void fixedUpdate(long timestamp);

    Players getPlayers();

    Clock getClock();

    Bolts getBolts();

    ScoreBoard getScoreBoard();


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

            Topics.setGame(this);
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
