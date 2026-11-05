package fr.paulm.projects.rover.telemetry;

// What a rover sends at the end of each of its instructions: where it is and how it is, at what time of the mission,
// with the dust in the sky (its optical depth) and how clean its solar panels are (1 without panels).
// sequence numbers the messages of a rover in a mission, from 0: a gap or a repeat shows a lost or doubled message.
public record RoverTelemetry(String missionId, String rover, long sequence, long missionSeconds, String missionClock,
                             double latitude, double longitude, int cellX, int cellY, String direction,
                             double batteryPercent, boolean night, double distanceMeters, double dustOpacity,
                             double panelCleanliness) {
}
