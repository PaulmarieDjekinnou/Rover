package fr.paulm.projects.rover.domain;

// How a rover's mission ended: its final state, and how many moves were blocked by another rover.
public record MissionResult(Rover finalRover, int blockedMoves) {
}
