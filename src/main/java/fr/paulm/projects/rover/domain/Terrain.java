package fr.paulm.projects.rover.domain;

// What the ground lets a rover do: whether it can drive from a cell to a neighbouring one.
// The surface says where a move leads; the terrain says whether the ground allows it.
public interface Terrain {

    // Ground with no relief, where every move is possible: the plateau of the first versions.
    Terrain FLAT = (from, to) -> true;

    boolean canDrive(Position from, Position to);
}
