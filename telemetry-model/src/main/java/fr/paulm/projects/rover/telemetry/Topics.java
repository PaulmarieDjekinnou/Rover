package fr.paulm.projects.rover.telemetry;

// The Kafka topics of the telemetry, shared by the simulator that writes them and the ground station that reads them.
public final class Topics {

    public static final String TELEMETRY = "rover.telemetry";
    public static final String EVENTS = "rover.events";
    public static final String STATUS = "mission.status";
    public static final String PLANS = "rover.plans";
    // The messages the ground station could not read, with the reason, so that they are not lost
    public static final String DEAD_LETTERS = "rover.dead-letters";

    private Topics() {
    }
}
