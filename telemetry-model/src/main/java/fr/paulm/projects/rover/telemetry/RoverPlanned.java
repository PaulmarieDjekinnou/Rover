package fr.paulm.projects.rover.telemetry;

// The plan of a rover, sent at the start of a mission: where it aims, its power source and its battery,
// when it lands (the mission time) and how many waypoints it has.
public record RoverPlanned(String missionId, String rover, double siteLatitude, double siteLongitude, String power,
                           double batteryWattHours, long landingSeconds, int waypoints) {
}
