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

    // A move is ignored when it would leave the plateau, so the rover is never lost,
    // or when the cell ahead is occupied by another rover, so two rovers never collide.
    // Ignored moves are counted, and the user is warned at the end of the mission, once per reason.
    // occupiedCells holds the positions of the other rovers; it is only read, never modified.
    Rover execute(List<Instruction> instructions, Plateau plateau, Set<Position> occupiedCells) {
        Rover current = this;
        int movesOffPlateau = 0;
        int movesIntoRover = 0;
        for (Instruction instruction : instructions) {
            if (instruction == Instruction.MOVE) {
                Position ahead = current.direction.from(current.position);
                if (!plateau.contains(ahead)) {
                    movesOffPlateau++;
                    continue;
                }
                if (occupiedCells.contains(ahead)) {
                    movesIntoRover++;
                    continue;
                }
            }
            current = current.apply(instruction);
        }
        if (movesOffPlateau > 0) {
            LOGGER.warn("Rover starting at \"{}\": {} move(s) ignored because they would leave the plateau.",
                    this, movesOffPlateau);
        }
        if (movesIntoRover > 0) {
            LOGGER.warn("Rover starting at \"{}\": {} move(s) ignored because the cell is occupied by another rover.",
                    this, movesIntoRover);
        }
        return current;
    }

    // Only called once the instruction is known to keep the rover on the plateau.
    private Rover apply(Instruction instruction) {
        return switch (instruction) {
            case LEFT -> new Rover(position, direction.left());
            case RIGHT -> new Rover(position, direction.right());
            case MOVE -> new Rover(direction.from(position), direction);
        };
    }

    // The output format of the program: "x y direction".
    @Override
    public String toString() {
        return position + " " + direction;
    }
}
