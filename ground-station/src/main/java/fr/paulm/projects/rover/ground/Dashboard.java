package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

// What mission control shows: the last known state of each rover of the latest mission, as the telemetry tells it.
// A rover is "silent" when it has sent nothing for a sol (of mission time) while the others went on.
final class Dashboard {

    private static final long SOL_SECONDS = 88_775;

    // The last telemetry and the last event of each rover, by name, for the mission being followed
    private final Map<String, RoverTelemetry> telemetry = new TreeMap<>();
    private final Map<String, RoverEvent> events = new TreeMap<>();
    private String missionId;
    private long latestSeconds;
    private String latestClock = "";

    void telemetry(RoverTelemetry message) {
        follow(message.missionId());
        telemetry.put(message.rover(), message);
        if (message.missionSeconds() >= latestSeconds) {
            latestSeconds = message.missionSeconds();
            latestClock = message.missionClock();
        }
    }

    void event(RoverEvent message) {
        follow(message.missionId());
        events.put(message.rover(), message);
    }

    // A new mission replaces the one followed so far.
    private void follow(String mission) {
        if (!mission.equals(missionId)) {
            missionId = mission;
            telemetry.clear();
            events.clear();
            latestSeconds = 0;
            latestClock = "";
        }
    }

    // The screen: the mission time, then one line per rover.
    List<String> render() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "%-14s mission %s", latestClock, missionId == null ? "-" : missionId));
        for (Map.Entry<String, RoverTelemetry> entry : telemetry.entrySet()) {
            RoverTelemetry rover = entry.getValue();
            lines.add(String.format(Locale.ROOT, "%-13s %7.2f %7.2f  %s  %s %3.0f %%  %-5s  %s", entry.getKey(),
                    rover.latitude(), rover.longitude(), rover.direction(), bar(rover.batteryPercent()),
                    rover.batteryPercent(), rover.night() ? "night" : "day", status(entry.getKey(), rover)));
        }
        events.keySet().stream().filter(name -> !telemetry.containsKey(name)).forEach(name ->
                lines.add(String.format(Locale.ROOT, "%-13s %s", name, events.get(name).type().name().toLowerCase(Locale.ROOT))));
        return lines;
    }

    private String status(String name, RoverTelemetry rover) {
        if (latestSeconds - rover.missionSeconds() > SOL_SECONDS) {
            return "silent";
        }
        RoverEvent event = events.get(name);
        return event == null || event.type() == RoverEvent.Type.LANDED ? "active" : event.type().name().toLowerCase(Locale.ROOT);
    }

    private String bar(double percent) {
        int full = (int) Math.round(percent / 10);
        return "#".repeat(full) + ".".repeat(10 - full);
    }
}
