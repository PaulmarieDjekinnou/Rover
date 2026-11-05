package fr.paulm.projects.rover.telemetry;

import java.time.Instant;

// The start or the end of a mission: which file, which planet, which seed, and when it ran (on the computer's clock).
public record MissionStatus(String missionId, Type type, String file, String planet, long seed, Instant wallClock) {

    public enum Type {
        STARTED, ENDED
    }
}
