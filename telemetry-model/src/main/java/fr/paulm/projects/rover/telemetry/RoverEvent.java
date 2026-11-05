package fr.paulm.projects.rover.telemetry;

// Something that happens to a rover: it landed, it was lost at landing, a move was blocked, it stopped for good.
// sequence numbers the events of a rover in a mission, from 0, like the telemetry.
public record RoverEvent(String missionId, String rover, long sequence, long missionSeconds, String missionClock,
                         Type type, String detail) {

    public enum Type {
        LANDED, LOST, BLOCKED, STOPPED
    }
}
