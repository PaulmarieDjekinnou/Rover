package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverPlanned;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.TelemetryJson;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReceiverTest {

    // Keeps what it is given, in order
    private static final class ListArchive implements Archive {
        final List<Object> written = new ArrayList<>();

        @Override
        public void status(MissionStatus message) {
            written.add(message);
        }

        @Override
        public void telemetry(RoverTelemetry message) {
            written.add(message);
        }

        @Override
        public void event(RoverEvent message) {
            written.add(message);
        }

        @Override
        public void plan(RoverPlanned message) {
            written.add(message);
        }

        @Override
        public void commit() {
        }

        @Override
        public void close() {
        }
    }

    private final ListArchive archive = new ListArchive();
    private final Receiver receiver = new Receiver(archive);

    @Test
    void eachTopicGoesToItsTable() {
        MissionStatus status = new MissionStatus("m1", MissionStatus.Type.STARTED, "f", "Mars", 88_775, 1, Instant.EPOCH);
        RoverEvent event = new RoverEvent("m1", "Spirit", 0, 0, "Sol 0 00:00", RoverEvent.Type.LANDED, "here");
        RoverTelemetry telemetry = new RoverTelemetry("m1", "Spirit", 0, 60, "Sol 0 00:01", 1, 2, 3, 4, "N", 90, false, 10, 0.5, 1);
        receiver.receive("mission.status", TelemetryJson.write(status));
        receiver.receive("rover.events", TelemetryJson.write(event));
        receiver.receive("rover.telemetry", TelemetryJson.write(telemetry));
        assertEquals(List.of(status, event, telemetry), archive.written);
        assertEquals(Map.of("m1", 1L), receiver.telemetryPerMission());
    }

    @Test
    void anUnknownTopicOrAnInvalidMessageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> receiver.receive("other", "{}"));
        assertThrows(IllegalArgumentException.class, () -> receiver.receive("rover.events", "not json"));
    }
}
