package me.paulo.casaes.bbop.service.kafka;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaProducerType {
    Type value();

    enum Type {
        FAST,
        BLOCK,
    }
}
