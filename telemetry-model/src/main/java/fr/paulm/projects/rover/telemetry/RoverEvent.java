package fr.paulm.projects.rover.telemetry;

// Something that happens to a rover: it landed, it was lost at landing, no route reaches one of its waypoints,
// a move was blocked, a dust storm started or ended where it is, the wind cleaned its solar panels, it stopped
// for good.
// sequence numbers the events of a rover in a mission, from 0, like the telemetry.
public record RoverEvent(String missionId, String rover, long sequence, long missionSeconds, String missionClock,
                         Type type, String detail) {

    public enum Type {
        LANDED, LOST, NO_ROUTE, BLOCKED_BY_TERRAIN, BLOCKED_BY_ROVER, DUST_STORM_STARTED, DUST_STORM_ENDED, PANELS_CLEANED,
        STOPPED
    }
}
