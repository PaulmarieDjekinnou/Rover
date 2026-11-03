package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

// The state of a rover at a given moment: its position and direction.
// Immutable: executing instructions returns a new state and leaves this one unchanged.
public record Rover(Position position, Direction direction) {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rover.class);

    // The plateau wraps around (see Plateau.wrap), so a rover never leaves it.
    // A move is ignored when the cell ahead is occupied by another rover, so two rovers never collide.
    // Ignored moves are counted, and the user is warned once, at the end of the mission.
    // occupiedCells holds the positions of the other rovers; it is only read, never modified.
    Rover execute(List<Instruction> instructions, Plateau plateau, Set<Position> occupiedCells) {
        Rover current = this;
        int blockedMoves = 0;
        for (Instruction instruction : instructions) {
            Rover next = current.apply(instruction, plateau);
            // Only a move changes the position, and the rover's own cell is never in occupiedCells
            if (occupiedCells.contains(next.position)) {
                blockedMoves++;
                continue;
            }
            current = next;
        }
        if (blockedMoves > 0) {
            LOGGER.warn("Rover starting at \"{}\": {} move(s) ignored because the cell is occupied by another rover.",
                    this, blockedMoves);
        }
        return current;
    }

    private Rover apply(Instruction instruction, Plateau plateau) {
        return switch (instruction) {
            case LEFT -> new Rover(position, direction.left());
            case RIGHT -> new Rover(position, direction.right());
            case MOVE -> new Rover(plateau.wrap(direction.from(position)), direction);
        };
    }

    // The output format of the program: "x y direction".
    @Override
    public String toString() {
        return position + " " + direction;
    }
}
