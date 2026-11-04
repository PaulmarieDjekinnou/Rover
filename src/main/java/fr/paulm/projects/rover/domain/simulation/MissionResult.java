package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;
import java.util.Optional;

// How a rover's mission ended: its final state, how many of its moves were blocked by another rover and by the
// terrain, the distance it drove, the time it spent driving and turning, the time it spent waiting (see Pace),
// and, if it stopped before the end of its instructions because it could no longer act, when it did
// (on the common clock).
public record MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain,
                            double distanceMeters, Duration drivingTime, Duration waitingTime, Optional<Duration> stopTime) {

    // A mission where nothing makes the rover wait.
    public MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain,
                         double distanceMeters, Duration drivingTime) {
        this(finalRover, blockedByRovers, blockedByTerrain, distanceMeters, drivingTime, Duration.ZERO, Optional.empty());
    }

    // A mission on ground with no scale, where nothing is measured.
    public MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain) {
        this(finalRover, blockedByRovers, blockedByTerrain, 0, Duration.ZERO);
    }

    // A mission on flat ground, where only rovers can block a move.
    public MissionResult(Rover finalRover, int blockedByRovers) {
        this(finalRover, blockedByRovers, 0);
    }

    public boolean stopped() {
        return stopTime.isPresent();
    }

    // The time from the start of the rover to the end of its last instruction. A rover that stopped may have been
    // busy with the instruction it gave up on: that time is neither driving nor waiting, see stopTime.
    public Duration totalTime() {
        return drivingTime.plus(waitingTime);
    }
}
