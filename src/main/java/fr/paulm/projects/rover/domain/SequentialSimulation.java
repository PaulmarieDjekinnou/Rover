package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// The rovers move one after the other: each rover executes all its instructions before the next one starts.
// Each instruction executed is a turn.
public final class SequentialSimulation implements Simulation {

    // The instruction played in a turn, and the rover (its index in input order) that plays it.
    private record Move(int rover, Instruction instruction) {
    }

    @Override
    public Stream<Turn> turns(SimulationInput input) {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < input.missions().size(); i++) {
            for (Instruction instruction : input.missions().get(i).instructions()) {
                moves.add(new Move(i, instruction));
            }
        }
        // Turn 0 is the starting state; each turn is computed from the previous one, only when it is read
        Turn start = new Turn(0, input.missions().stream().map(RoverMission::rover).toList(), Set.of());
        return Stream.iterate(start, turn -> next(turn, moves.get(turn.number()), input.plateau()))
                .skip(1)
                .limit(moves.size());
    }

    // All rovers are on the plateau from the start: a rover cannot move onto the cell of a rover
    // that has finished (its final position) or that has not started yet (its starting position).
    // Both are exactly the positions of the other rovers in the previous turn.
    private Turn next(Turn previous, Move move, Plateau plateau) {
        List<Rover> rovers = new ArrayList<>(previous.rovers());
        Set<Position> otherRoversCells = new HashSet<>();
        for (int i = 0; i < rovers.size(); i++) {
            if (i != move.rover()) {
                otherRoversCells.add(rovers.get(i).position());
            }
        }
        Optional<Rover> next = rovers.get(move.rover()).step(move.instruction(), plateau, otherRoversCells);
        next.ifPresent(rover -> rovers.set(move.rover(), rover));
        return new Turn(previous.number() + 1, rovers, next.isPresent() ? Set.of() : Set.of(move.rover()));
    }
}
