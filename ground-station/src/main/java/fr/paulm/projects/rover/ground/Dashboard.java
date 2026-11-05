package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// What mission control shows: every mission it has heard of, the most recent first, each with its own view.
final class Dashboard {

    // By mission id, in the order the missions were first heard of
    private final Map<String, MissionView> missions = new LinkedHashMap<>();

    void telemetry(RoverTelemetry message) {
        view(message.missionId()).telemetry(message);
    }

    void status(MissionStatus message) {
        view(message.missionId()).status(message);
    }

    void event(RoverEvent message) {
        view(message.missionId()).event(message);
    }

    private MissionView view(String missionId) {
        return missions.computeIfAbsent(missionId, MissionView::new);
    }

    // The screen: each mission, the most recent first, separated by a blank line.
    List<String> render() {
        List<String> lines = new ArrayList<>();
        for (MissionView mission : recentFirst()) {
            if (!lines.isEmpty()) {
                lines.add("");
            }
            lines.addAll(mission.render());
        }
        return lines;
    }

    // Every mission as JSON, the most recent first, for the web page.
    List<Map<String, Object>> json() {
        return recentFirst().stream().map(MissionView::json).toList();
    }

    private List<MissionView> recentFirst() {
        return new ArrayList<>(missions.values()).reversed();
    }
}
