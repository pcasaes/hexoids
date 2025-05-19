package me.pcasaes.hexoids.core.application.eventhandlers;

import me.pcasaes.hexoids.core.domain.model.DomainEvent;

import java.util.function.BooleanSupplier;

public interface ApplicationConsumers {


     void onJoinGame(DomainEvent domainEvent);

     void onPlayerAction(DomainEvent domainEvent);

     void onBoltLifeCycle(DomainEvent domainEvent) ;

     void onBoltAction(DomainEvent domainEvent);

     void onScoreBoardControl(DomainEvent domainEvent);

     void onScoreBoardUpdate(DomainEvent domainEvent);

    interface HaveStarted extends BooleanSupplier {
    }

}
