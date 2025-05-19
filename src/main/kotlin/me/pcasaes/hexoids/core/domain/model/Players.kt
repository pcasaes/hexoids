package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.config.Config.Companion.get
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.of
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.model.physics.Shockwave.Companion.shipExploded
import pcasaes.hexoids.proto.BoltsAvailableCommandDto
import pcasaes.hexoids.proto.CurrentViewCommandDto
import pcasaes.hexoids.proto.DirectedCommand
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.PlayerDestroyedEventDto
import pcasaes.hexoids.proto.PlayerJoinedEventDto
import java.util.Optional
import java.util.UUID
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * The collection of Players.
 */
class Players private constructor(
    private val bolts: Bolts,
    private val clock: Clock,
    private val scoreBoard: ScoreBoard,
    private val barriers: Barriers,
    private val physicsQueue: PhysicsQueueEnqueue,
    private val spatialIndexFactory: PlayerSpatialIndexFactory
) : Iterable<Player> {
    /**
     * All players are maintained in this map
     */
    private val playerMap = HashMap<EntityId, Player>(5000, 0.5F)

    /**
     * Only players connected to this node are maintained in this set
     */
    private val playerServerUpdateSet = HashSet<EntityId>(5000, 0.5F)

    private val registeredCurrentViewModifiers = HashMap<EntityId, Consumer<CurrentViewCommandDto.Builder>>()

    /**
     * If the player hasn't been created will do so and return the player
     *
     * @param id player's id.
     * @return If created returns the player
     */
    fun createPlayer(id: EntityId): Optional<Player> {
        if (playerMap.containsKey(id)) {
            return Optional.empty()
        }
        return Optional.of(createOrGet(id))
    }

    /**
     * Returns a player, creating them if they don't exist.
     *
     * @param id the player's id.
     * @return the player
     */
    fun createOrGet(id: EntityId): Player {
        return playerMap.computeIfAbsent(id) { id -> this.create(id) }
    }

    /**
     * Returns a specific player if they exist.
     *
     *
     *
     * @param id player's identifier
     * @return Returns the player if they exist
     */
    fun get(id: EntityId): Optional<Player> {
        return Optional.ofNullable(playerMap[id])
    }

    private fun get(id: UUID): Optional<Player> {
        return get(of(id))
    }

    private fun create(id: EntityId): Player {
        requestCurrentView(id)
        return Player.create(id, this, this.bolts, this.barriers, this.clock, this.scoreBoard)
    }

    fun registerCurrentViewModifier(modifierId: EntityId, builderConsumer: Consumer<CurrentViewCommandDto.Builder>) {
        registeredCurrentViewModifiers.put(modifierId, builderConsumer)
    }

    fun unregisterCurrentViewModifier(modifierId: EntityId) {
        registeredCurrentViewModifiers.remove(modifierId)
    }

    /**
     * Requests the game's current view to be sent to a specific player
     *
     * @param requesterId player to have the current view sent to.
     */
    fun requestCurrentView(requesterId: EntityId) {
        val currentViewBuilder = CurrentViewCommandDto.newBuilder()
            .addAllBarriers(
                StreamSupport.stream(barriers.spliterator(), false)
                    .map { obj -> obj.toDto() }
                    .collect(Collectors.toList())
            )
            .setBoltsAvailable(BoltsAvailableCommandDto.newBuilder().setAvailable(get().getMaxBolts()))

        registeredCurrentViewModifiers
            .values
            .forEach { c -> c.accept(currentViewBuilder) }

        playerMap
            .values
            .stream()
            .map { obj -> obj.toDtoIfJoined() }
            .filter { obj -> obj.isPresent }
            .map { obj -> obj.get() }
            .forEach { value -> currentViewBuilder.addPlayers(value) }

        val builder = DirectedCommand.newBuilder()
            .setPlayerId(requesterId.getGuid())
            .setCurrentView(currentViewBuilder)

        getClientEvents().dispatch(
            Dto.newBuilder()
                .setDirectedCommand(builder)
                .build()
        )
    }

    /**
     * Iterator of players
     *
     * @return
     */
    override fun iterator(): MutableIterator<Player> {
        return playerMap
            .values
            .iterator()
    }

    /**
     * Stream of players
     *
     * @return
     */
    fun stream(): Stream<Player> {
        return StreamSupport.stream(spliterator(), false)
    }

    fun joined(event: PlayerJoinedEventDto) {
        val player = createOrGet(of(event.playerId))
        player.joined(event)
    }

    private fun left(playerId: EntityId) {
        val player = createOrGet(playerId)
        playerMap.remove(playerId)
        playerServerUpdateSet.remove(playerId)
        player.left()
    }

    fun consumeFromJoinTopic(domainEvent: DomainEvent) {
        if (domainEvent.event == null) {
            left(of(domainEvent.key))
        } else if (domainEvent.event.hasPlayerJoined()) {
            joined(domainEvent.event.getPlayerJoined())
        }
    }

    fun consumeFromPlayerActionTopic(domainEvent: DomainEvent) {
        if (domainEvent.event != null && domainEvent.event.hasPlayerMoved()) {
            get(domainEvent.key)
                .ifPresent { p -> p.moved(domainEvent.event.getPlayerMoved()) }
        }
        if (domainEvent.event != null && domainEvent.event.hasPlayerDestroyed()) {
            get(domainEvent.key)
                .ifPresent { p -> handleDestroyed(p, domainEvent.event.getPlayerDestroyed()) }
        }
        if (domainEvent.event != null && domainEvent.event.hasPlayerSpawned()) {
            get(domainEvent.key)
                .ifPresent { p -> p.spawned(domainEvent.event.getPlayerSpawned()) }
        }
    }

    fun handleDestroyed(player: Player, playerDestroyedEvent: PlayerDestroyedEventDto) {
        handleShockwave(player, playerDestroyedEvent.destroyedTimestamp)
        player.destroyed(playerDestroyedEvent)
    }

    /**
     * When a player is destroyed we generate a shockwave that pushes nearby players away
     * from the destroyed players last position.
     *
     * @param fromPlayer
     */
    fun handleShockwave(fromPlayer: Player, destroyedAt: Long) {
        this.physicsQueue.enqueue(shipExploded(fromPlayer, this, destroyedAt))
    }

    /**
     * This call marks the player is connected on this running instance.
     * This is used to update server calculated vector positions.
     *
     * @param playerId
     */
    fun connected(playerId: EntityId) {
        playerServerUpdateSet.add(playerId)
    }

    fun consumeFromPlayerFiredTopic(domainEvent: DomainEvent) {
        if (domainEvent.event != null && domainEvent.event.hasPlayerFired()) {
            val playerFiredEventDto = domainEvent.event.getPlayerFired()
            get(of(playerFiredEventDto.ownerPlayerId))
                .ifPresent { p -> p.fired(domainEvent.event.getPlayerFired()) }
        }
    }

    fun consumeFromBoltActionTopic(domainEvent: DomainEvent) {
        if (domainEvent.event != null && domainEvent.event.hasBoltExhausted()) {
            val event = domainEvent.event.getBoltExhausted()
            get(of(event.ownerPlayerId))
                .ifPresent { obj -> obj.boltExhausted() }
        }
    }

    /**
     * Updates all connected player models' vector positions.
     *
     * @param timestamp the timestamp to update the players to.
     */
    fun fixedUpdate(timestamp: Long) {
        playerServerUpdateSet
            .stream()
            .map { key -> playerMap[key] }
            .forEach { p -> p?.fixedUpdate(timestamp) }
    }

    /**
     * Return the total number of players in the game.
     * Is weakly consistent and thread safe.
     *
     * @return
     */
    fun getTotalNumberOfPlayers(): Int {
        return this.playerMap.size
    }

    /**
     * Return the number of players connected this node.
     * Is weakly consistent and thread safe.
     *
     * @return
     */
    fun getNumberOfConnectedPlayers(): Int {
        return this.playerServerUpdateSet.size
    }

    fun hasConnectedPlayers(): Boolean {
        return this.playerServerUpdateSet.isNotEmpty()
    }

    fun getSpatialIndex(): PlayerSpatialIndex {
        return spatialIndexFactory.get()
    }

    fun isConnected(id: EntityId): Boolean {
        return playerServerUpdateSet.contains(id)
    }

    companion object {
        @JvmStatic
        fun create(
            bolts: Bolts,
            clock: Clock,
            scoreBoard: ScoreBoard,
            barriers: Barriers,
            physicsQueue: PhysicsQueueEnqueue,
            spatialIndexFactory: PlayerSpatialIndexFactory
        ): Players {
            return Players(bolts, clock, scoreBoard, barriers, physicsQueue, spatialIndexFactory)
        }
    }
}
