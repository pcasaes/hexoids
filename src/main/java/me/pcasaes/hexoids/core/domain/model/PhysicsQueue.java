package me.pcasaes.hexoids.core.domain.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongConsumer;

public interface PhysicsQueue extends PhysicsQueueEnqueue {

    void fixedUpdate(long timestamp);

    static PhysicsQueue create() {
        return new Implementation();
    }

    class Implementation implements PhysicsQueue {

        private final Deque<LongConsumer> queue = new ArrayDeque<>();

        @Override
        public void enqueue(LongConsumer action) {
            this.queue.offer(action);
        }

        @Override
        public void fixedUpdate(long timestamp) {
            LongConsumer last = queue.peekLast();
            if (last == null) {
                return;
            }

            LongConsumer consumer;
            do {
                consumer = queue.pollFirst();
                consumer.accept(timestamp);

            } while (consumer != last);
        }
    }
}
