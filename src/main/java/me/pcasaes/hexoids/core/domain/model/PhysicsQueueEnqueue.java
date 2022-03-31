package me.pcasaes.hexoids.core.domain.model;

import java.util.function.LongPredicate;

public interface PhysicsQueueEnqueue {

    void enqueue(LongPredicate action);

}
