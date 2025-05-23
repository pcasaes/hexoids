package me.pcasaes.hexoids.core.domain.model

import java.util.function.LongPredicate

fun interface PhysicsQueueEnqueue {
    /**
     * Enqueues an action into the physics queue.
     * @param action    Receives timestamp. On return true will re-enqueue to execute on next physics iteration
     */
    fun enqueue(action: LongPredicate)
}
