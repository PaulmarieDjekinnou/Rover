package fr.paulm.projects.rover.telemetry;

// Something that happens to a rover: it landed, it was lost at landing, a move was blocked, it stopped for good.
public record RoverEvent(String missionId, String rover, long missionSeconds, String missionClock, Type type,
                         String detail) {

    public enum Type {
        LANDED, LOST, BLOCKED, STOPPED
    }
}
