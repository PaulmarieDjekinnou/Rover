package fr.paulm.projects.rover.domain;

// The shape of the world the rovers drive on: which cells exist, and how they connect.
// The surface alone knows what is beyond an edge, so it computes both the next cell and the direction
// the rover has when it gets there.
public interface Surface {

    boolean contains(Position position);

    // Where a rover arrives when it leaves this cell going in this direction.
    Heading next(Position position, Direction direction);
}
