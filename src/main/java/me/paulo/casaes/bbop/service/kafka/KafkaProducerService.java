package me.paulo.casaes.bbop.service.kafka;

import me.paulo.casaes.bbop.service.kafka.converter.UUIDBytesSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.UUID;

public class KafkaProducerService {

    private KafkaConfiguration configuration;

    private Producer<UUID, String> producer;

    KafkaProducerService(KafkaConfiguration configuration) {
        this.configuration = configuration;
    }

    KafkaProducerService start(boolean fast) {
        this.producer = fast ? createFastProducer() : createBlockProducer();
        return this;
    }

    void stop() {
        this.producer.close();
    }

    public void send(String topic, UUID key, String message) {
        this.producer.send(new ProducerRecord<>(
                topic,
                key,
                message
        ));
    }

    private Properties getConfig() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.configuration.getConnectionUrl());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDBytesSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }

    private Producer<UUID, String> createBlockProducer() {
        Properties properties = getConfig();
        properties.setProperty(CommonClientConfigs.CLIENT_ID_CONFIG, UUID.randomUUID().toString() + "-guaranteed-" + this.configuration.getClientIdSuffix());
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");

        return new KafkaProducer<>(properties);
    }

    private Producer<UUID, String> createFastProducer() {
        Properties properties = getConfig();
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "0");
        properties.setProperty(CommonClientConfigs.CLIENT_ID_CONFIG, UUID.randomUUID().toString() + "-fast-" + this.configuration.getClientIdSuffix());

        return new KafkaProducer<>(properties);
    }

}
