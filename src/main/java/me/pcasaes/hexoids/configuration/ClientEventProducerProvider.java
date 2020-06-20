package me.pcasaes.hexoids.configuration;

import me.pcasaes.hexoids.entrypoints.web.ClientBroadcaster;
import me.pcasaes.hexoids.infrastructure.producer.ClientEventProducer;
import pcasaes.hexoids.proto.Dto;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@ApplicationScoped
public class ClientEventProducerProvider {

    private final ClientBroadcaster clientBroadcaster;

    @Inject
    public ClientEventProducerProvider(ClientBroadcaster clientBroadcaster) {
        this.clientBroadcaster = clientBroadcaster;
    }

    @Produces
    @Singleton
    public ClientEventProducer getClientBroadcaster() {
        return new ClientEventProducer() {
            @Override
            public boolean isEnabled() {
                return clientBroadcaster.isEnabled();
            }

            @Override
            public void accept(Dto dto) {
                clientBroadcaster.accept(dto);
            }

            @Override
            public String getName() {
                return clientBroadcaster.getName();
            }
        };
    }
}
