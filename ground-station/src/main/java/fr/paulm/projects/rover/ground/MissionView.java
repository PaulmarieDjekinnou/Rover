package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

// What mission control shows of one mission: the last known state of each of its rovers, as the telemetry tells it.
// A rover is "silent" when it has sent nothing for a sol of its planet (of mission time) while the others went on;
// once the mission has ended, every rover is "done".
final class MissionView {

    // Until the start of the mission tells it, the sol of Mars
    private long solSeconds = 88_775;
    private boolean ended;

    // The last telemetry and the last event of each rover, by name, for the mission being followed
    private final Map<String, RoverTelemetry> telemetry = new TreeMap<>();
    private final Map<String, RoverEvent> events = new TreeMap<>();
    private final String missionId;
    private long latestSeconds;
    private String latestClock = "";

    MissionView(String missionId) {
        this.missionId = missionId;
    }

    void telemetry(RoverTelemetry message) {
        telemetry.put(message.rover(), message);
        if (message.missionSeconds() >= latestSeconds) {
            latestSeconds = message.missionSeconds();
            latestClock = message.missionClock();
        }
    }

    void status(MissionStatus message) {
        solSeconds = message.solSeconds();
        ended = message.type() == MissionStatus.Type.ENDED;
    }

    void event(RoverEvent message) {
        events.put(message.rover(), message);
    }

    // The screen: the mission time, then one line per rover.
    List<String> render() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "%-14s mission %s%s", latestClock, missionId, ended ? " (ended)" : ""));
        for (Map.Entry<String, RoverTelemetry> entry : telemetry.entrySet()) {
            RoverTelemetry rover = entry.getValue();
            lines.add(String.format(Locale.ROOT, "%-13s %7.2f %7.2f  %s  %s %3.0f %%  %-5s  dust %3.1f  %s", entry.getKey(),
                    rover.latitude(), rover.longitude(), rover.direction(), bar(rover.batteryPercent()),
                    rover.batteryPercent(), rover.night() ? "night" : "day", rover.dustOpacity(),
                    status(entry.getKey(), rover)));
        }
        events.keySet().stream().filter(name -> !telemetry.containsKey(name)).forEach(name ->
                lines.add(String.format(Locale.ROOT, "%-13s %s", name, events.get(name).type().name().toLowerCase(Locale.ROOT))));
        return lines;
    }

    // The mission as JSON for the web page: its time, whether it has ended, and each rover with its position.
    Map<String, Object> json() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("missionId", missionId);
        mission.put("clock", latestClock);
        mission.put("ended", ended);
        List<Map<String, Object>> rovers = new ArrayList<>();
        telemetry.forEach((name, rover) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("latitude", rover.latitude());
            row.put("longitude", rover.longitude());
            row.put("direction", rover.direction());
            row.put("battery", rover.batteryPercent());
            row.put("night", rover.night());
            row.put("dust", rover.dustOpacity());
            row.put("status", status(name, rover));
            rovers.add(row);
        });
        mission.put("rovers", rovers);
        return mission;
    }

    private String status(String name, RoverTelemetry rover) {
        RoverEvent stopped = events.get(name);
        if (stopped != null && stopped.type() == RoverEvent.Type.STOPPED) {
            return "stopped";
        }
        if (ended) {
            return "done";
        }
        if (latestSeconds - rover.missionSeconds() > solSeconds) {
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
