package fr.paulm.projects.rover.domain;

// What can stop a rover from moving: another rover on the cell it wants, or the ground between the two cells.
public enum Obstacle {
    ROVER, TERRAIN
}
