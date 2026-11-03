package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

// The state of a rover at a given moment: its position and direction.
// Immutable: executing instructions returns a new state and leaves this one unchanged.
public record Rover(Position position, Direction direction) {

    // The plateau wraps around (see Plateau.wrap), so a rover never leaves it.
    // A move (forward or back) is ignored when the target cell is occupied by another rover, so two rovers never collide.
    // occupiedCells holds the positions of the other rovers; it is only read, never modified.
    MissionResult execute(List<Instruction> instructions, Plateau plateau, Set<Position> occupiedCells) {
        Rover current = this;
        int blockedMoves = 0;
        for (Instruction instruction : instructions) {
            Optional<Rover> next = current.step(instruction, plateau, occupiedCells);
            if (next.isPresent()) {
                current = next.get();
            } else {
                blockedMoves++;
            }
        }
        return new MissionResult(current, blockedMoves);
    }

    // The rover after one instruction, or empty when the instruction would move it onto an occupied cell.
    // occupiedCells must not contain the rover's own cell, otherwise a turn on the spot would be blocked.
    Optional<Rover> step(Instruction instruction, Plateau plateau, Set<Position> occupiedCells) {
        Rover next = apply(instruction, plateau);
        // Only a move (forward or back) changes the position
        return occupiedCells.contains(next.position) ? Optional.empty() : Optional.of(next);
    }

    private Rover apply(Instruction instruction, Plateau plateau) {
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
