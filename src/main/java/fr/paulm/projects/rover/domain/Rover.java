package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// The state of a rover at a given moment: its position and direction.
// Immutable: executing instructions returns a new state and leaves this one unchanged.
public record Rover(Position position, Direction direction) {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rover.class);

    // A move that would leave the plateau is ignored, so the rover is never lost.
    // Ignored moves are counted, and the user is warned once per rover, at the end of its mission.
    Rover execute(List<Instruction> instructions, Plateau plateau) {
        Rover current = this;
        int ignoredMoves = 0;
        for (Instruction instruction : instructions) {
            if (instruction == Instruction.MOVE && !current.canMoveForward(plateau)) {
                ignoredMoves++;
                continue;
            }
            current = current.apply(instruction);
        }
        if (ignoredMoves > 0) {
            LOGGER.warn("Rover starting at \"{}\": {} move(s) ignored because they would leave the plateau.",
                    this, ignoredMoves);
        }
        return current;
    }

    private boolean canMoveForward(Plateau plateau) {
        return plateau.contains(direction.from(position));
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
