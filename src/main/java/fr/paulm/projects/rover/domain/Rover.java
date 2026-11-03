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

    Rover execute(List<Instruction> instructions, Plateau plateau) {
        Rover current = this;
        for (Instruction instruction : instructions) {
            current = current.apply(instruction, plateau);
        }
        return current;
    }

    private Rover apply(Instruction instruction, Plateau plateau) {
        return switch (instruction) {
            case LEFT -> new Rover(position, direction.left());
            case RIGHT -> new Rover(position, direction.right());
            case MOVE -> move(plateau);
        };
    }

    // A move that would leave the plateau is ignored, so the rover is never lost; the user is warned.
    private Rover move(Plateau plateau) {
        Position next = direction.from(position);
        if (!plateau.contains(next)) {
            LOGGER.warn("Rover at \"{}\" cannot move to \"{}\": move ignored.", this, next);
            return this;
        }
        return new Rover(next, direction);
    }

    // The output format of the program: "x y direction".
    @Override
    public String toString() {
        return position + " " + direction;
    }
}
