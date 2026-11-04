package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.KafkaSink;
import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.TelemetryJson;

import java.util.HashMap;
import java.util.Map;

// Reads one message of a topic and gives it to the archive. Counts the telemetry received per mission,
// for the summary of the station.
final class Receiver {
    private final Archive archive;
    private final Map<String, Long> telemetryPerMission = new HashMap<>();

    Receiver(Archive archive) {
        this.archive = archive;
    }

    void receive(String topic, String json) {
        switch (topic) {
            case KafkaSink.TELEMETRY_TOPIC -> {
                RoverTelemetry message = TelemetryJson.read(json, RoverTelemetry.class);
                archive.telemetry(message);
                telemetryPerMission.merge(message.missionId(), 1L, Long::sum);
            }
            case KafkaSink.EVENTS_TOPIC -> archive.event(TelemetryJson.read(json, RoverEvent.class));
            case KafkaSink.STATUS_TOPIC -> archive.status(TelemetryJson.read(json, MissionStatus.class));
            default -> throw new IllegalArgumentException("Unknown topic: " + topic);
        }
    }

    Map<String, Long> telemetryPerMission() {
        return Map.copyOf(telemetryPerMission);
    }
}
