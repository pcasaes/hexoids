package me.pcasaes.hexoids.service.kafka;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaProducerType {
    Type value();

    enum Type {
        /**
         * The producer will fire and forget
         */
        FAST,

        /**
         * The producer will block until kafka acks.
         */
        BLOCK,
    }
}
