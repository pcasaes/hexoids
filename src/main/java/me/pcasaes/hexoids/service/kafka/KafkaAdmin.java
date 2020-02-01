package me.pcasaes.hexoids.service.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Properties;
import java.util.function.Consumer;

@Dependent
public class KafkaAdmin {

    private final KafkaConfiguration configuration;

    @Inject
    public KafkaAdmin(KafkaConfiguration configuration) {
        this.configuration = configuration;
    }

    public void execute(Consumer<AdminClient> adminClientConsumer) {
        try (AdminClient adminClient = AdminClient.create(properties())) {
            adminClientConsumer.accept(adminClient);
        }
    }

    private Properties properties() {
        Properties properties = new Properties();
        properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.configuration.getConnectionUrl());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.TRUE.toString());
        properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");

        return properties;
    }


}
