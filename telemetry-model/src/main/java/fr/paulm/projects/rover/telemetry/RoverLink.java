package fr.paulm.projects.rover.telemetry;

// The link to Earth of a rover so far: how many telemetry messages and events reached Earth, how many were lost
// (the gaps in their sequence numbers, up to the highest one received), when the latest one arrived, and the longest
// a message took from the rover to Earth.
public record RoverLink(String missionId, String rover, long telemetry, long events, long lostTelemetry,
                        long lostEvents, long lastTelemetrySequence, long lastEventSequence, long lastReceivedSeconds,
                        long longestWaitSeconds) {
}
