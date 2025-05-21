package me.pcasaes.hexoids.core.domain.model

import pcasaes.hexoids.proto.Dto
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.logging.Logger

open class GameEvents<T> private constructor(private val name: String) {
    private val dispatcher = AtomicReference<Consumer<T>?>()

    /**
     * The game model generates events that must be delivered to other instances
     * of the game model (horizontal scaling) or broadcast to clients.
     *
     *
     * The domain model does not concern itself with how this is done, only that it is done.
     * This method is used to register infrastructure code to dispatch event.
     *
     * @param dispatcher
     */
    fun registerEventDispatcher(dispatcher: Consumer<T>?) {
        this.dispatcher.set(dispatcher)
    }

    private fun getDispatcher(): Consumer<T> {
        var currentDispatcher = this.dispatcher.getPlain()
        if (currentDispatcher != null) {
            return currentDispatcher
        }
        currentDispatcher = this.dispatcher.get()
        if (currentDispatcher == null) {
            // this is a noop dispatcher and should never actually be used
            return Consumer { _ -> LOGGER.severe("NO DISPATCHER REGISTERED FOR " + this.name) }
        }
        return currentDispatcher
    }

    fun dispatch(event: T) {
        getDispatcher().accept(event)
    }


    private object DtoGameEvents : GameEvents<Dto>("client")
    private object DomainGameEvents : GameEvents<DomainEvent>("domain-event")


    companion object {
        private val LOGGER: Logger = Logger.getLogger(GameEvents::class.java.getName())

        fun getClientEvents(): GameEvents<Dto> {
            return DtoGameEvents
        }

        fun getDomainEvents(): GameEvents<DomainEvent> {
            return DomainGameEvents
        }
    }
}
