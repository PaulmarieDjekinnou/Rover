package fr.paulm.projects.rover.telemetry;

// What a pass of a relay orbiter brought to Earth from a rover: the messages that left at that pass all reach Earth
// at the same time, receivedSeconds. How many telemetry messages and events it carried, and the mission times of the
// oldest and the newest of them: the oldest one waited receivedSeconds - oldestSeconds in the rover's memory and
// on its way.
public record RelayPass(String missionId, String rover, long receivedSeconds, long telemetry, long events,
                        long oldestSeconds, long newestSeconds) {
}
