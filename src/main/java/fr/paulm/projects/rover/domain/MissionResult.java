package fr.paulm.projects.rover.domain;

// How a rover's mission ended: its final state, and how many moves were ignored
// because the cell ahead was occupied by another rover.
public record MissionResult(Rover finalRover, int blockedMoves) {
}
