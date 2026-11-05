package fr.paulm.projects.rover.domain;

import java.util.Optional;
import java.util.Set;

// The state of a rover at a given moment: its position and direction.
// Immutable: executing instructions returns a new state and leaves this one unchanged.
public record Rover(Position position, Direction direction) {

    // What stops this rover from becoming next, if anything: first the terrain between the two cells, then another
    // rover on the cell it wants, so two rovers never collide. occupiedCells holds the positions of the rovers;
    // it is only read, never modified. It may contain the rover's own cell: a rover that stays on its cell
    // (a turn) is never stopped.
    public Optional<Obstacle> obstacleTo(Rover next, Terrain terrain, Set<Position> occupiedCells) {
        if (next.position.equals(position)) {
            return Optional.empty();
        }
        if (!terrain.canDrive(position, next.position)) {
            return Optional.of(Obstacle.TERRAIN);
        }
        return occupiedCells.contains(next.position) ? Optional.of(Obstacle.ROVER) : Optional.empty();
    }

    // The rover after one instruction, without looking at the other rovers.
    public Rover apply(Instruction instruction, Surface surface) {
        return switch (instruction) {
            case LEFT -> new Rover(position, direction.left());
            case RIGHT -> new Rover(position, direction.right());
            case MOVE -> {
                Heading heading = surface.next(position, direction);
                yield new Rover(heading.position(), heading.direction());
            }
            // The rover drives backwards: it travels the opposite way and keeps facing away from where it goes,
            // so if the surface turns its travel around (across a pole), it turns the rover around too
            case BACK -> {
                Heading heading = surface.next(position, direction.opposite());
                yield new Rover(heading.position(), heading.direction().opposite());
            }
        };
    }

    // The output format of the program: "x y direction".
    @Override
    public String toString() {
        return position + " " + direction;
    }
}
