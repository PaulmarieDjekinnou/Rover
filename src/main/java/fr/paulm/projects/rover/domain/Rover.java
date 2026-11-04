package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.Optional;
import java.util.Set;

// The state of a rover at a given moment: its position and direction.
// Immutable: executing instructions returns a new state and leaves this one unchanged.
public record Rover(Position position, Direction direction) {

    // The rover after one instruction, or empty when the instruction would move it onto an occupied cell,
    // so two rovers never collide. The plateau wraps around (see Plateau.wrap), so a rover never leaves it.
    // occupiedCells holds the positions of the rovers; it is only read, never modified. It may contain
    // the rover's own cell: a rover that stays on its cell (a turn) is never blocked.
    Optional<Rover> step(Instruction instruction, Plateau plateau, Set<Position> occupiedCells) {
        Rover next = apply(instruction, plateau);
        boolean blocked = !next.position.equals(position) && occupiedCells.contains(next.position);
        return blocked ? Optional.empty() : Optional.of(next);
    }

    // The rover after one instruction, without looking at the other rovers.
    Rover apply(Instruction instruction, Plateau plateau) {
        return switch (instruction) {
            case LEFT -> new Rover(position, direction.left());
            case RIGHT -> new Rover(position, direction.right());
            case MOVE -> new Rover(plateau.wrap(direction.ahead(position)), direction);
            case BACK -> new Rover(plateau.wrap(direction.behind(position)), direction);
        };
    }

    // The output format of the program: "x y direction".
    @Override
    public String toString() {
        return position + " " + direction;
    }
}
