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
// The time on Earth is the latest time a message reached it: with the link to Earth, what is known of a rover is
// hours old, and each rover shows how old. A message that arrives late never replaces a newer state.
// A rover is "silent" when nothing from it has reached Earth for a sol of its planet while the others went on;
// once the mission has ended, every rover is "done".
final class MissionView {

    // Until the start of the mission tells it, the sol of Mars
    private long solSeconds = 88_775;
    private boolean ended;

    // The last telemetry and the last event of each rover, by name, for the mission being followed
    private final Map<String, RoverTelemetry> telemetry = new TreeMap<>();
    private final Map<String, RoverEvent> events = new TreeMap<>();
    private final String missionId;
    // The latest time a message reached Earth, in seconds of the mission
    private long earthSeconds;

    MissionView(String missionId) {
        this.missionId = missionId;
    }

    void telemetry(RoverTelemetry message) {
        telemetry.merge(message.rover(), message, (known, received) -> received.sequence() >= known.sequence() ? received : known);
        earthSeconds = Math.max(earthSeconds, message.receivedSeconds());
    }

    void status(MissionStatus message) {
        solSeconds = message.solSeconds();
        ended = message.type() == MissionStatus.Type.ENDED;
    }

    void event(RoverEvent message) {
        events.merge(message.rover(), message, (known, received) -> received.sequence() >= known.sequence() ? received : known);
        earthSeconds = Math.max(earthSeconds, message.receivedSeconds());
    }

    // The screen: the time on Earth, then one line per rover.
    List<String> render() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "%-14s mission %s%s", clock(), missionId, ended ? " (ended)" : ""));
        for (Map.Entry<String, RoverTelemetry> entry : telemetry.entrySet()) {
            RoverTelemetry rover = entry.getValue();
            lines.add(String.format(Locale.ROOT, "%-13s %7.2f %7.2f  %s  %s %3.0f %%  %-5s  dust %3.1f  %-14s %s",
                    entry.getKey(), rover.latitude(), rover.longitude(), rover.direction(), bar(rover.batteryPercent()),
                    rover.batteryPercent(), rover.night() ? "night" : "day", rover.dustOpacity(),
                    age(earthSeconds - rover.missionSeconds()) + " old", status(entry.getKey(), rover)));
        }
        events.keySet().stream().filter(name -> !telemetry.containsKey(name)).forEach(name ->
                lines.add(String.format(Locale.ROOT, "%-13s %s", name, events.get(name).type().name().toLowerCase(Locale.ROOT))));
        return lines;
    }

    // The mission as JSON for the web page: its time on Earth, whether it has ended, and each rover with its position
    // and how old it is.
    Map<String, Object> json() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("missionId", missionId);
        mission.put("clock", clock());
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
            row.put("age", age(earthSeconds - rover.missionSeconds()));
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
        if (earthSeconds - rover.receivedSeconds() > solSeconds) {
            return "silent";
        }
        RoverEvent event = events.get(name);
        return event == null || event.type() == RoverEvent.Type.LANDED ? "active" : event.type().name().toLowerCase(Locale.ROOT);
    }

    // The time on Earth, as the mission clock gives it: the sol, then the time of day, a sol being cut into 24 hours.
    private String clock() {
        long minutes = (earthSeconds % solSeconds) * 24 * 60 / solSeconds;
        return String.format(Locale.ROOT, "Sol %d %02d:%02d", earthSeconds / solSeconds, minutes / 60, minutes % 60);
    }

    // How long ago, in hours and minutes of the planet, or in sols beyond a sol.
    private String age(long seconds) {
        if (seconds >= solSeconds) {
            long sols = seconds / solSeconds;
            return sols + (sols == 1 ? " sol" : " sols");
        }
        long minutes = seconds * 24 * 60 / solSeconds;
        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private String bar(double percent) {
        int full = (int) Math.round(percent / 10);
        return "#".repeat(full) + ".".repeat(10 - full);
    }
}
