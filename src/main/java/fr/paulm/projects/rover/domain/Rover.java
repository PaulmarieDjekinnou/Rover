package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.List;

// The state of a rover at a given moment: its position and heading.
// Immutable: executing instructions returns a new state and leaves this one unchanged.
public record Rover(int x, int y, Direction heading) {

    Rover execute(List<Instruction> instructions, Plateau plateau) {
        Rover current = this;
        for (Instruction instruction : instructions) {
            current = current.apply(instruction, plateau);
        }
        return current;
    }

    private Rover apply(Instruction instruction, Plateau plateau) {
        return switch (instruction) {
            case L -> new Rover(x, y, heading.left());
            case R -> new Rover(x, y, heading.right());
            case M -> move(plateau);
        };
    }

    // A move that would leave the plateau is ignored, so the rover is never lost.
    private Rover move(Plateau plateau) {
        int nx = x + heading.dx;
        int ny = y + heading.dy;
        return plateau.contains(nx, ny) ? new Rover(nx, ny, heading) : this;
    }

    // The output format of the program: "x y heading".
    @Override
    public String toString() {
        return x + " " + y + " " + heading;
    }
}
