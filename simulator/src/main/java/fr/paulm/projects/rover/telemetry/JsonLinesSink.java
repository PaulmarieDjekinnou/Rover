package fr.paulm.projects.rover.telemetry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

// Writes the telemetry to a file instead of Kafka, one message per line (JSON Lines), with the topic and the key
// it would have in Kafka: {"topic":"rover.telemetry","key":"Curiosity","message":{...}}.
// To compare two runs, to look at a mission without Kafka, or to replay it into Kafka later.
public final class JsonLinesSink implements TelemetrySink {
    private final BufferedWriter writer;

    public JsonLinesSink(Path file) {
        try {
            writer = Files.newBufferedWriter(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void telemetry(RoverTelemetry message) {
        write(Topics.TELEMETRY, message.rover(), message);
    }

    @Override
    public void event(RoverEvent message) {
        write(Topics.EVENTS, message.rover(), message);
    }

    @Override
    public void status(MissionStatus message) {
        write(Topics.STATUS, message.missionId(), message);
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void write(String topic, String key, Object message) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("topic", topic);
        line.put("key", key);
        line.put("message", message);
        try {
            writer.write(TelemetryJson.write(line));
            writer.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
