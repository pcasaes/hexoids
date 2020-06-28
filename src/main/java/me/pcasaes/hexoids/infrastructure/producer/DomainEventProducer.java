package me.pcasaes.hexoids.infrastructure.producer;

import me.pcasaes.hexoids.core.domain.model.DomainEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.Event;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

/**
 * Used to generate domain events. Domain events are used to keep server nodes in sync
 */
@ApplicationScoped
public class DomainEventProducer {

    private static final String NAME = "domain-event-producer";


    private final String kafkaBroker;

    private Producer<UUID, Event> producer;

    @Inject

    public DomainEventProducer(
            @ConfigProperty(name = "kafka.bootstrap.servers") String kafkaBroker
    ) {
        this.kafkaBroker = kafkaBroker;
    }

    @PostConstruct
    public void start() {
        this.producer = createProducer();
    }

    @PreDestroy
    public void stop() {
        this.producer.close(Duration.ofSeconds(5));
    }

    public void accept(DomainEvent event) {
        if (event != null) {
            this.producer.send(new ProducerRecord<>(event.getTopic(), event.getKey(), event.getEvent()));
        }
    }

    public String getName() {
        return NAME;
    }

    public Producer<UUID, Event> createProducer() {

        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaBroker);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "me.pcasaes.hexoids.infrastructure.kafka.converter.UUIDBytesSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "me.pcasaes.hexoids.infrastructure.kafka.converter.EventDtoSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, "0");

        return new KafkaProducer<>(props);

    }
}
