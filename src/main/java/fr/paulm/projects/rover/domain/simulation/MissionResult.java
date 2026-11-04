package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;

// How a rover's mission ended: its final state, how many of its moves were blocked by another rover
// and by the terrain, the distance it drove and the time it spent driving and turning.
public record MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain,
                            double distanceMeters, Duration drivingTime) {

    // A mission on ground with no scale, where nothing is measured.
    public MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain) {
        this(finalRover, blockedByRovers, blockedByTerrain, 0, Duration.ZERO);
    }

    // A mission on flat ground, where only rovers can block a move.
    public MissionResult(Rover finalRover, int blockedByRovers) {
        this(finalRover, blockedByRovers, 0);
    }
}
