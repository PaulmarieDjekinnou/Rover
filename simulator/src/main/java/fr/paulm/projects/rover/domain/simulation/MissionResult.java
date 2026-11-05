package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;
import java.util.Optional;

// How a rover's mission ended: its final state, how many of its moves were blocked by another rover and by the
// terrain, the distance it drove, the time it spent driving and turning, the time it spent waiting (see Pace),
// and, if it stopped before the end of its instructions because it could no longer act, when it did
// (on the common clock).
// A rover that stopped during a move also says which cell it was driving to, and which part of the way it had done.
public record MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain,
                            double distanceMeters, Duration drivingTime, Duration waitingTime, Optional<Duration> stopTime,
                            Optional<Partway> partway) {

    // The cell a rover was driving to when it stopped, and the part of the way it had done (from 0 to 1)
    public record Partway(Position toward, double part) {
    }

    // A rover that did not stop during a move.
    public MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain, double distanceMeters,
                         Duration drivingTime, Duration waitingTime, Optional<Duration> stopTime) {
        this(finalRover, blockedByRovers, blockedByTerrain, distanceMeters, drivingTime, waitingTime, stopTime,
                Optional.empty());
    }

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

    // The time from the start of the rover to the end of its last instruction, or to the moment it gave up.
    // A rover that stopped counts the part of its last instruction it drove, but stays in the cell it left.
    public Duration totalTime() {
        return drivingTime.plus(waitingTime);
    }
}
