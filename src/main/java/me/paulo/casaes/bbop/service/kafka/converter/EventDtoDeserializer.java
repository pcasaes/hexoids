package me.paulo.casaes.bbop.service.kafka.converter;

import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.service.DtoProcessorService;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;

public class EventDtoDeserializer implements Deserializer<EventDto> {

    private final DtoProcessorService processor = new DtoProcessorService();
    private final StringDeserializer stringDeserializer = new StringDeserializer();


    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        stringDeserializer.configure(configs, isKey);
    }

    @Override
    public EventDto deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        final String value = stringDeserializer.deserialize(topic, data);

        return processor.getEventType(value)
                .map(eventType -> processor.deserialize(value, eventType.getClassType()))
                .orElse(null);

    }

    @Override
    public void close() {
        stringDeserializer.close();
    }
}
