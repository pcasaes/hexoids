package me.pcasaes.hexoids.service.kafka;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class KafkaConfiguration {

    private final String connectionUrl;

    private final String clientIdSuffix;

    @Inject
    public KafkaConfiguration(
            @ConfigProperty(
                    name = "hexoids.config.kafka.connection.url",
                    defaultValue = "hexoids-kafka:9092"
            ) String connectionUrl,

            @ConfigProperty(
                    name = "hexoids.config.kafka.client.id.suffix",
                    defaultValue = "hexoids"
            ) String clientIdSuffix) {
        this.connectionUrl = connectionUrl;
        this.clientIdSuffix = clientIdSuffix;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getClientIdSuffix() {
        return clientIdSuffix;
    }
}
