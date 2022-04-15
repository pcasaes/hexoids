package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory;
import me.pcasaes.hexoids.core.domain.metrics.PhysicsMetrics;
import me.pcasaes.hexoids.core.domain.model.physics.Blackhole;

import java.util.function.LongConsumer;

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
     * Returns the barriers singleton.
     *
     * @return Barriers
     */
    Barriers getBarriers();

    PhysicsQueueEnqueue getPhysicsQueue();

    PhysicsMetrics getPhysicsMetrics();

    /**
     * Returns the game's singleton.
     *
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
            PhysicsQueue physicsQueue = PhysicsQueue.create();
            EventScheduler eventScheduler = EventScheduler.create(physicsQueue);
            Barriers barriers = Barriers.create();
            Players players = Players.create(bolts, clock, scoreBoard, barriers, physicsQueue, PlayerSpatialIndexFactory.factory());

            INSTANCE = new Implementation(players, clock, bolts, scoreBoard, barriers, eventScheduler, physicsQueue);
        }


        private final Players players;

        private final Clock clock;

        private final Bolts bolts;

        private final ScoreBoard scoreBoard;

        private final Barriers barriers;

        private final EventScheduler eventScheduler;

        private final PhysicsQueue physicsQueue;

        private final LongConsumer barriersUpdate;

        private final LongConsumer playersUpdate;

        private final LongConsumer boltsUpdate;

        private final LongConsumer scoreBoardUpdate;

        private final LongConsumer physicsQueueUpdate;

        private final LongConsumer eventSchedulerUpdate;

        private Implementation(Players players, Clock clock, Bolts bolts, ScoreBoard scoreBoard, Barriers barriers, EventScheduler eventScheduler, PhysicsQueue physicsQueue) {
            this.players = players;
            this.clock = clock;
            this.bolts = bolts;
            this.scoreBoard = scoreBoard;
            this.barriers = barriers;
            this.eventScheduler = eventScheduler;
            this.physicsQueue = physicsQueue;

            this.barriersUpdate = PhysicsMetrics.get().intercept(barriers::fixedUpdate, "barriers");
            this.playersUpdate = PhysicsMetrics.get().intercept(players::fixedUpdate, "players");
            this.boltsUpdate = PhysicsMetrics.get().intercept(bolts::fixedUpdate, "bolts");
            this.scoreBoardUpdate = PhysicsMetrics.get().intercept(scoreBoard::fixedUpdate, "score-board");
            this.eventSchedulerUpdate = PhysicsMetrics.get().intercept(eventScheduler::fixedUpdate, "event-scheduler");
            this.physicsQueueUpdate = PhysicsMetrics.get().intercept(physicsQueue::fixedUpdate, "physics-queue");

            eventScheduler.register((r, s, e) -> Blackhole.massCollapsed(r, s, e, clock, players));

            GameTopic.setGame(this);
        }


        @Override
        public void fixedUpdate(long timestamp) {
            barriersUpdate.accept(timestamp);
            playersUpdate.accept(timestamp);
            boltsUpdate.accept(timestamp);
            scoreBoardUpdate.accept(timestamp);
            eventSchedulerUpdate.accept(timestamp);
            physicsQueueUpdate.accept(timestamp);
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

        @Override
        public Barriers getBarriers() {
            return barriers;
        }

        @Override
        public PhysicsQueueEnqueue getPhysicsQueue() {
            return physicsQueue;
        }

        @Override
        public PhysicsMetrics getPhysicsMetrics() {
            return PhysicsMetrics.get();
        }
    }
}
