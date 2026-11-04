package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

// How a rover's mission ended: its final state, and how many of its moves were blocked by another rover
// and by the terrain.
public record MissionResult(Rover finalRover, int blockedByRovers, int blockedByTerrain) {

    // A mission on flat ground, where only rovers can block a move.
    public MissionResult(Rover finalRover, int blockedByRovers) {
        this(finalRover, blockedByRovers, 0);
    }
}
