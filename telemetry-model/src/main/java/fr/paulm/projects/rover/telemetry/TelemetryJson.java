package fr.paulm.projects.rover.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// The messages as JSON, one object per message: readable with the Kafka command-line tools.
// A message must have all its fields, none of them null: by default, Jackson would fill a missing field with null or
// 0, and "{}" would be read as a message of nobody, from no mission.
public final class TelemetryJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES);

    private TelemetryJson() {
    }

    public static String write(Object message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write the message " + message, e);
        }
    }

    // The message of the given type in this JSON; an IllegalArgumentException if the JSON is not such a message.
    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid " + type.getSimpleName() + ": " + e.getOriginalMessage(), e);
        }
    }
}
