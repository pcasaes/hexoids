package me.pcasaes.hexoids.core.domain.model;

import java.util.function.LongConsumer;

public interface PhysicsQueueEnqueue {

    void enqueue(LongConsumer action);

}
