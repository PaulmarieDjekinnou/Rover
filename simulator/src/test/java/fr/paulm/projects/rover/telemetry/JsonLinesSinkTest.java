package fr.paulm.projects.rover.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JsonLinesSinkTest {

    @TempDir
    Path tempDir;

    @Test
    void writesOneLinePerMessageWithItsTopicAndKey() throws Exception {
        Path file = tempDir.resolve("mission.jsonl");
        RoverTelemetry telemetry = new RoverTelemetry("m1", "Spirit", 0, 60, "Sol 0 00:01", 1, 2, 3, 4, "N", 90, false, 10);
        try (TelemetrySink sink = TelemetrySinks.open("file:" + file)) {
            assertInstanceOf(JsonLinesSink.class, sink);
            sink.status(new MissionStatus("m1", MissionStatus.Type.STARTED, "f", "Mars", 1, Instant.EPOCH));
            sink.telemetry(telemetry);
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        JsonNode second = new ObjectMapper().readTree(lines.get(1));
        assertEquals("rover.telemetry", second.get("topic").asText());
        assertEquals("Spirit", second.get("key").asText());
        assertEquals(telemetry, TelemetryJson.read(second.get("message").toString(), RoverTelemetry.class));
    }
}
