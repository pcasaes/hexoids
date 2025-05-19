package me.pcasaes.hexoids.core.domain.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongPredicate;

public interface PhysicsQueue extends PhysicsQueueEnqueue {

    int fixedUpdate(long timestamp);

    static PhysicsQueue create() {
        return new Implementation();
    }

    class Implementation implements PhysicsQueue {

        private int enqueuedCount = 0;
        private final Deque<LongPredicate> queue = new ArrayDeque<>();

        @Override
        public void enqueue(LongPredicate action) {
            this.queue.offerLast(action);
            this.enqueuedCount++;
        }

        private LongPredicate poll() {
            enqueuedCount--;
            return queue.pollFirst();
        }

        @Override
        public int fixedUpdate(long timestamp) {

            final int processUpTo = enqueuedCount;
            for (int i = 0; i < processUpTo; i++) {
                LongPredicate proc = poll();
                if (proc != null && proc.test(timestamp)) {
                    this.enqueue(proc);
                }
            }
            return processUpTo;
        }
    }
}
