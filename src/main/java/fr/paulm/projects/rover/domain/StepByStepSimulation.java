package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// The rovers move turn by turn: in each turn, every rover executes its next instruction, in input order.
// A rover that has no instruction left stays on its cell until the end.
public final class StepByStepSimulation implements Simulation {

    @Override
    public Stream<Turn> turns(SimulationInput input) {
        int turns = input.missions().stream().mapToInt(mission -> mission.instructions().size()).max().orElse(0);
        // Turn 0 is the starting state; each turn is computed from the previous one, only when it is read
        Turn start = new Turn(0, input.missions().stream().map(RoverMission::rover).toList(), Set.of());
        return Stream.iterate(start, turn -> next(turn, input)).skip(1).limit(turns);
    }

    private Turn next(Turn previous, SimulationInput input) {
        // The turn numbered n plays the instruction of index n - 1 of each rover
        int index = previous.number();
        List<Rover> rovers = new ArrayList<>(previous.rovers());
        Set<Position> occupiedCells = new HashSet<>();
        rovers.forEach(rover -> occupiedCells.add(rover.position()));
        Set<Integer> blockedRovers = new HashSet<>();
        for (int i = 0; i < rovers.size(); i++) {
            List<Instruction> instructions = input.missions().get(i).instructions();
            if (index >= instructions.size()) {
                continue;
            }
            Rover rover = rovers.get(i);
            // The rover's own cell is not an obstacle for itself
            occupiedCells.remove(rover.position());
            Optional<Rover> next = rover.step(instructions.get(index), input.plateau(), occupiedCells);
            if (next.isPresent()) {
                rover = next.get();
                rovers.set(i, rover);
            } else {
                blockedRovers.add(i);
            }
            occupiedCells.add(rover.position());
        }
        return new Turn(index + 1, rovers, blockedRovers);
    }
}
