package fr.paulm.projects.rover.telemetry;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryJsonTest {

    private final RoverTelemetry telemetry = new RoverTelemetry("m1", "Curiosity", 3, 655_432, "Sol 7 09:51",
            -4.99, 137.45, 549, 340, "S", 80, true, 29_580);

    @Test
    void aMessageBecomesJsonAndBack() {
        String json = TelemetryJson.write(telemetry);
        assertTrue(json.contains("\"rover\":\"Curiosity\""), json);
        assertEquals(telemetry, TelemetryJson.read(json, RoverTelemetry.class));
    }

    @Test
    void timesAndTypesAreWrittenAsText() {
        MissionStatus status = new MissionStatus("m1", MissionStatus.Type.STARTED, "mission.txt", "Mars", 42,
                Instant.parse("2026-11-04T10:00:00Z"));
        String json = TelemetryJson.write(status);
        assertTrue(json.contains("\"wallClock\":\"2026-11-04T10:00:00Z\""), json);
        assertTrue(json.contains("\"type\":\"STARTED\""), json);
        assertEquals(status, TelemetryJson.read(json, MissionStatus.class));
    }

    @Test
    void invalidJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> TelemetryJson.read("{not json", RoverEvent.class));
    }
}
