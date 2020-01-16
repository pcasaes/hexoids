package me.pcasaes.bbop.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.pcasaes.bbop.dto.CommandType;
import me.pcasaes.bbop.dto.EventType;

import javax.enterprise.context.Dependent;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

@Dependent
public class DtoProcessorService {

    private static final Logger LOGGER = Logger.getLogger(DtoProcessorService.class.getName());


    private static final ObjectMapper SERIALIZER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.INDENT_OUTPUT, false);

    private static final ObjectMapper DESERIALIZER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String serializeToString(Object dto) {
        try {
            return SERIALIZER.writeValueAsString(dto);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public <T> T deserialize(String value, Class<T> type) {
        try {
            return DESERIALIZER.readValue(value, type);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public Optional<EventType> getEventType(String value) {
        try {
            JsonNode node = DESERIALIZER.readTree(value);
            if (node.has("event")) {
                return Optional.of(EventType.valueOf(node.get("event").asText()));
            }
        } catch (IOException | RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<CommandType> getCommand(String value) {
        try {
            JsonNode node = DESERIALIZER.readTree(value);
            if (!node.has("command")) {
                return Optional.empty();
            }
            return Optional.ofNullable(CommandType.valueOf(node.get("command").asText()));
        } catch (IOException | RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
            return Optional.empty();
        }
    }

}
