package me.pcasaes.hexoids.core.domain.model

import java.util.ArrayDeque
import java.util.function.LongPredicate

interface PhysicsQueue : PhysicsQueueEnqueue {
    fun fixedUpdate(timestamp: Long): Int

    class Implementation : PhysicsQueue {
        private var enqueuedCount = 0
        private val queue = ArrayDeque<LongPredicate>()

        override fun enqueue(action: LongPredicate) {
            this.queue.offerLast(action)
            this.enqueuedCount++
        }

        private fun poll(): LongPredicate? {
            enqueuedCount--
            return queue.pollFirst()
        }

        override fun fixedUpdate(timestamp: Long): Int {
            val processUpTo = enqueuedCount
            for (i in 0..<processUpTo) {
                val proc = poll()
                if (proc != null && proc.test(timestamp)) {
                    this.enqueue(proc)
                }
            }
            return processUpTo
        }
    }

    companion object {
        fun create(): PhysicsQueue {
            return Implementation()
        }
    }
}
