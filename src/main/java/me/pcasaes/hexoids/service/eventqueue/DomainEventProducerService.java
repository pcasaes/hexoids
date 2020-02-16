package me.pcasaes.hexoids.service.eventqueue;

import com.google.protobuf.GeneratedMessageLite;
import me.pcasaes.hexoids.model.DomainEvent;
import me.pcasaes.hexoids.service.kafka.KafkaProducerService;
import me.pcasaes.hexoids.service.kafka.KafkaProducerType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
public class DomainEventProducerService implements EventQueueConsumerService<DomainEvent> {

    private KafkaProducerService producerService;

    @Inject
    public DomainEventProducerService(
            @KafkaProducerType(KafkaProducerType.Type.FAST) KafkaProducerService producerService
    ) {
        this.producerService = producerService;
    }

    private byte[] serialize(GeneratedMessageLite<?, ?> value) {
        return value.toByteArray();
    }

    @Override
    public void accept(DomainEvent event) {
        if (event != null) {
            byte[] message = event.getEvent() == null ? null : serialize(event.getEvent());
            this.producerService.send(event.getTopic(), event.getKey(), message);
        }
    }

    @Override
    public String getName() {
        return "domain-event-producer";
    }
}
