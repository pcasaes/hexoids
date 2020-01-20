package me.pcasaes.bbop.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.pcasaes.bbop.dto.CommandType;
import me.pcasaes.bbop.dto.EventType;

import javax.enterprise.context.Dependent;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

@Dependent
public class DtoProcessorService {

    private static final Logger LOGGER = Logger.getLogger(DtoProcessorService.class.getName());


    private static final ObjectMapper SERIALIZER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.INDENT_OUTPUT, false);

    private static final ObjectWriter WRITER = SERIALIZER.writer();

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


    /**
     * Creates a Json Writer with a reusable buffer. Not thread safe.
     * @return
     */
    public JsonWriter createJsonWriter() {
        return JsonWriter.create();
    }

    public static class JsonWriter implements Closeable {

        private final JsonGenerator jsonGenerator;
        private final ByteArrayOutputStream outputStream;

        private JsonWriter(JsonGenerator jsonGenerator, ByteArrayOutputStream outputStream) {
            this.jsonGenerator = jsonGenerator;
            this.outputStream = outputStream;
        }

        private static JsonWriter create() {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                return new JsonWriter(SERIALIZER.getFactory().createGenerator(outputStream), outputStream);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public void close() throws IOException {
            jsonGenerator.close();
            outputStream.close();
        }

        public String writeValue(Object value) throws IOException {
            outputStream.reset();
            WRITER.writeValue(jsonGenerator, value);
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

}
