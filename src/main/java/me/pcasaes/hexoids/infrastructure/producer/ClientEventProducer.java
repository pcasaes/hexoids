package me.pcasaes.hexoids.infrastructure.producer;

import pcasaes.hexoids.proto.Dto;

/**
 * Produces events for clients
 */
public interface ClientEventProducer {

    boolean isEnabled();
    
    void accept(Dto dto);

    String getName();
}
