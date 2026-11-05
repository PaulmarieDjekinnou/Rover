package fr.paulm.projects.rover.domain;

import java.time.Duration;

// What the ground lets a rover do: whether it can drive from a cell to a neighbouring one, how far that is
// and how long it takes. The surface says where a move leads; the terrain says whether the ground allows it.
public interface Terrain {

    // Ground with no relief and no scale, where every move is possible: the plateau of the first versions.
    Terrain FLAT = (from, to) -> true;

    boolean canDrive(Position from, Position to);

    // The length of the drive between two neighbouring cells, in meters. None on ground with no scale.
    default double distance(Position from, Position to) {
        return 0;
    }

    // How long the drive between two neighbouring cells takes. No time on ground with no scale.
    default Duration driveTime(Position from, Position to) {
        return Duration.ZERO;
    }

    // How long a turn on the spot takes.
    default Duration turnTime() {
        return Duration.ZERO;
    }
}
