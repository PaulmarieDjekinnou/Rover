package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverPlanned;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.TelemetryJson;
import fr.paulm.projects.rover.telemetry.Topics;

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
            case Topics.TELEMETRY -> {
                RoverTelemetry message = TelemetryJson.read(json, RoverTelemetry.class);
                archive.telemetry(message);
                telemetryPerMission.merge(message.missionId(), 1L, Long::sum);
            }
            case Topics.EVENTS -> archive.event(TelemetryJson.read(json, RoverEvent.class));
            case Topics.STATUS -> archive.status(TelemetryJson.read(json, MissionStatus.class));
            case Topics.PLANS -> archive.plan(TelemetryJson.read(json, RoverPlanned.class));
            default -> throw new IllegalArgumentException("Unknown topic: " + topic);
        }
    }

    Map<String, Long> telemetryPerMission() {
        return Map.copyOf(telemetryPerMission);
    }
}
