package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// The state of a rover at a given moment: its position and heading.
// Immutable: executing instructions returns a new state and leaves this one unchanged.
public record Rover(int x, int y, Direction heading) {
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
            case LEFT -> new Rover(x, y, heading.left());
            case RIGHT -> new Rover(x, y, heading.right());
            case MOVE -> move(plateau);
        };
    }

    // A move that would leave the plateau is ignored, so the rover is never lost; the user is warned.
    private Rover move(Plateau plateau) {
        int nx = x + heading.dx;
        int ny = y + heading.dy;
        if (!plateau.contains(nx, ny)) {
            LOGGER.warn("Rover at \"{}\" cannot move to \"{} {}\": move ignored.", this, nx, ny);
            return this;
        }
        return new Rover(nx, ny, heading);
    }

    // The output format of the program: "x y heading".
    @Override
    public String toString() {
        return x + " " + y + " " + heading;
    }
}
