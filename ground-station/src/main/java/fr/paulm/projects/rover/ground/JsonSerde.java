package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.TelemetryJson;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

// A message as JSON in Kafka Streams, the same JSON as everywhere else. A message that cannot be read throws a
// SerializationException, which the deserialization handler of the application logs and skips.
final class JsonSerde<T> implements Serde<T> {
    private final Class<T> type;

    JsonSerde(Class<T> type) {
        this.type = type;
    }

    @Override
    public Serializer<T> serializer() {
        return (topic, message) -> message == null ? null : TelemetryJson.write(message).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Deserializer<T> deserializer() {
        return (topic, bytes) -> {
            if (bytes == null) {
                return null;
            }
            try {
                return TelemetryJson.read(new String(bytes, StandardCharsets.UTF_8), type);
            } catch (IllegalArgumentException e) {
                throw new SerializationException(e.getMessage(), e);
            }
        };
    }
}
