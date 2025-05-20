package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory.Companion.factory
import me.pcasaes.hexoids.core.domain.metrics.PhysicsMetrics
import me.pcasaes.hexoids.core.domain.model.EventScheduler.Companion.create
import me.pcasaes.hexoids.core.domain.model.physics.Blackhole
import java.util.Random
import java.util.function.LongConsumer

/**
 * The Game object is a singleton access through [Game.get] that is
 * the root object of the game composition.
 */
interface Game {
    /**
     * Updates the game model to the specified timestamp.
     *
     *
     * Will move players, bolts and check for hits and score updates.
     *
     * @param timestamp time to update the game model. Should always increase.
     */
    fun fixedUpdate(timestamp: Long)

    /**
     * Returns the players collection singleton.
     *
     * @return Players collection
     */
    fun getPlayers(): Players

    /**
     * Returns the game's clock.
     *
     * @return Game clock
     */
    fun getClock(): Clock

    /**
     * Returns the bolts collection singleton.
     *
     * @return Bolts collection
     */
    fun getBolts(): Bolts

    /**
     * Returns the score board singleton.
     *
     * @return Scoreboard
     */
    fun getScoreBoard(): ScoreBoard

    /**
     * Returns the barriers singleton.
     *
     * @return Barriers
     */
    fun getBarriers(): Barriers

    fun getPhysicsQueue(): PhysicsQueueEnqueue

    fun getPhysicsMetrics(): PhysicsMetrics

    class Implementation private constructor(
        private val players: Players,
        private val clock: Clock,
        private val bolts: Bolts,
        private val scoreBoard: ScoreBoard,
        private val barriers: Barriers,
        private val eventScheduler: EventScheduler,
        private val physicsQueue: PhysicsQueue
    ) : Game {

        private val barriersUpdate: LongConsumer

        private val playersUpdate: LongConsumer

        private val boltsUpdate: LongConsumer

        private val scoreBoardUpdate: LongConsumer

        private val physicsQueueUpdate: LongConsumer

        private val eventSchedulerUpdate: LongConsumer

        init {

            this.barriersUpdate = PhysicsMetrics.get()
                .intercept({ timestamp -> barriers.fixedUpdate(timestamp) }, "barriers")
            this.playersUpdate = PhysicsMetrics.get()
                .intercept({ timestamp -> players.fixedUpdate(timestamp) }, "players")
            this.boltsUpdate = PhysicsMetrics.get()
                .intercept({ timestamp -> bolts.fixedUpdate(timestamp) }, "bolts")
            this.scoreBoardUpdate = PhysicsMetrics.get()
                .intercept({ timestamp -> scoreBoard.fixedUpdate(timestamp) }, "score-board")
            this.eventSchedulerUpdate = PhysicsMetrics.get()
                .intercept({ timestamp -> eventScheduler.fixedUpdate(timestamp) }, "event-scheduler")
            this.physicsQueueUpdate = PhysicsMetrics.get()
                .intercept({ timestamp -> physicsQueue.fixedUpdate(timestamp) }, "physics-queue")

            eventScheduler.register { r: Random, s: Long, e: Long ->
                Blackhole.massCollapsed(
                    r, s, e, clock, players, bolts
                )
            }

            GameTopic.setGame(this)
        }


        override fun fixedUpdate(timestamp: Long) {
            barriersUpdate.accept(timestamp)
            playersUpdate.accept(timestamp)
            boltsUpdate.accept(timestamp)
            scoreBoardUpdate.accept(timestamp)
            eventSchedulerUpdate.accept(timestamp)
            physicsQueueUpdate.accept(timestamp)
        }

        override fun getPlayers(): Players {
            return players
        }

        override fun getClock(): Clock {
            return clock
        }

        override fun getBolts(): Bolts {
            return bolts
        }

        override fun getScoreBoard(): ScoreBoard {
            return scoreBoard
        }

        override fun getBarriers(): Barriers {
            return barriers
        }

        override fun getPhysicsQueue(): PhysicsQueueEnqueue {
            return physicsQueue
        }

        override fun getPhysicsMetrics(): PhysicsMetrics {
            return PhysicsMetrics.get()
        }

        companion object {
            val INSTANCE: Game

            init {
                val clock = Clock.create()
                val bolts = Bolts.create()
                val scoreBoard = ScoreBoard.create(clock)
                val physicsQueue = PhysicsQueue.create()
                val eventScheduler = create(physicsQueue)
                val barriers = Barriers.create()
                val players = Players.create(bolts, clock, scoreBoard, barriers, physicsQueue, factory())

                INSTANCE = Implementation(players, clock, bolts, scoreBoard, barriers, eventScheduler, physicsQueue)
            }
        }
    }

    companion object {
        /**
         * Returns the game's singleton.
         *
         * @return
         */
        fun get(): Game {
            return Implementation.Companion.INSTANCE
        }
    }
}
