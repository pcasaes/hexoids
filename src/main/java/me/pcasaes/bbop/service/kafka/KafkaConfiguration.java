package me.pcasaes.bbop.service.kafka;

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
                    name = "bbop.config.kafka.connection.url",
                    defaultValue = "bbop-kafka:9092"
            ) String connectionUrl,

            @ConfigProperty(
                    name = "bbop.config.kafka.client.id.suffix",
                    defaultValue = "bbop"
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
