package me.pcasaes.hexoids.infrastructure.kafka.converter;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import pcasaes.hexoids.proto.Event;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static me.pcasaes.hexoids.domain.model.DtoUtils.EVENT_THREAD_SAFE_BUILDER;

public class EventDtoDeserializer implements Deserializer<Event> {

    private static final Logger LOGGER = Logger.getLogger(EventDtoDeserializer.class.getName());
    private final StringDeserializer stringDeserializer = new StringDeserializer();


    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        stringDeserializer.configure(configs, isKey);
    }

    @Override
    public Event deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return EVENT_THREAD_SAFE_BUILDER
                    .get()
                    .clear()
                    .mergeFrom(data)
                    .build();
        } catch (InvalidProtocolBufferException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;

    }

    @Override
    public void close() {
        stringDeserializer.close();
    }
}
