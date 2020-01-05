package me.paulo.casaes.bbop.service.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class KafkaProducerProvider {


    private KafkaConfiguration configuration;

    public KafkaProducerProvider() {
    }

    @Inject
    public KafkaProducerProvider(KafkaConfiguration configuration) {
        this.configuration = configuration;
    }

    @Produces
    @KafkaProducerType(KafkaProducerType.Type.FAST)
    public KafkaProducerService getFastProducerService() {
        return new KafkaProducerService(configuration)
                .start(true);
    }

    @Produces
    @KafkaProducerType(KafkaProducerType.Type.BLOCK)
    public KafkaProducerService getBlockProducerService() {
        return new KafkaProducerService(configuration)
                .start(false);
    }

    public void dispose(@Disposes KafkaProducerService producerService) {
        producerService.stop();
    }
}
