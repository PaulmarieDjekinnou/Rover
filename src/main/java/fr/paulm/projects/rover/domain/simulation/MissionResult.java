package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;

// How a rover's mission ended: its final state, how many of its moves were blocked by another rover and by the
// terrain, the distance it drove, the time it spent driving and turning, the time it spent waiting (see Pace),
// and whether it stopped before the end of its instructions because it could no longer act.
public record MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain,
                            double distanceMeters, Duration drivingTime, Duration waitingTime, boolean stopped) {

    // A mission where nothing makes the rover wait.
    public MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain,
                         double distanceMeters, Duration drivingTime) {
        this(finalRover, blockedByRovers, blockedByTerrain, distanceMeters, drivingTime, Duration.ZERO, false);
    }

    // A mission on ground with no scale, where nothing is measured.
    public MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain) {
        this(finalRover, blockedByRovers, blockedByTerrain, 0, Duration.ZERO);
    }

    // A mission on flat ground, where only rovers can block a move.
    public MissionResult(Rover finalRover, int blockedByRovers) {
        this(finalRover, blockedByRovers, 0);
    }

    // The time from the start of the mission to the end of the rover's last instruction.
    public Duration totalTime() {
        return drivingTime.plus(waitingTime);
    }
}
