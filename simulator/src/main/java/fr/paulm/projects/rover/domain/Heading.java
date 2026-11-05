package fr.paulm.projects.rover.domain;

// Where a rover arrives when it leaves a cell, and which way it is then going.
// On a plateau the direction never changes; on a sphere, crossing a pole turns the rover around.
public record Heading(Position position, Direction direction) {
}
