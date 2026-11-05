package fr.paulm.projects.rover.telemetry;

// What a rover sends at the end of each of its instructions: where it is and how it is, at what time of the mission,
// with the dust in the sky (its optical depth) and how clean its solar panels are (1 without panels).
// sequence numbers the messages of a rover in a mission, from 0: a gap or a repeat shows a lost or doubled message.
// receivedSeconds is when the message reached Earth, in seconds of the mission: later than missionSeconds when the
// mission simulates the link to Earth (the rover waits for a relay orbiter, then light takes minutes to arrive).
public record RoverTelemetry(String missionId, String rover, long sequence, long missionSeconds, String missionClock,
                             double latitude, double longitude, int cellX, int cellY, String direction,
                             double batteryPercent, boolean night, double distanceMeters, double dustOpacity,
                             double panelCleanliness, long receivedSeconds) {

    // A message that reaches Earth at once.
    public RoverTelemetry(String missionId, String rover, long sequence, long missionSeconds, String missionClock,
                          double latitude, double longitude, int cellX, int cellY, String direction,
                          double batteryPercent, boolean night, double distanceMeters, double dustOpacity,
                          double panelCleanliness) {
        this(missionId, rover, sequence, missionSeconds, missionClock, latitude, longitude, cellX, cellY, direction,
                batteryPercent, night, distanceMeters, dustOpacity, panelCleanliness, missionSeconds);
    }

    public RoverTelemetry received(long seconds) {
        return new RoverTelemetry(missionId, rover, sequence, missionSeconds, missionClock, latitude, longitude, cellX,
                cellY, direction, batteryPercent, night, distanceMeters, dustOpacity, panelCleanliness, seconds);
    }
}
