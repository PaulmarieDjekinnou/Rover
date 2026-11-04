package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The rovers move turn by turn: in each turn, every rover executes its next instruction, in input order.
// A rover that has no instruction left stays on its cell until the end. Each turn is sent to the listener.
public final class StepByStepSimulation implements Simulation {

    @Override
    public List<MissionResult> run(SimulationInput input, TurnListener listener) {
        List<RoverMission> missions = input.missions();
        List<Rover> rovers = new ArrayList<>(missions.stream().map(RoverMission::rover).toList());
        int[] blockedMoves = new int[missions.size()];
        Set<Position> occupiedCells = new HashSet<>();
        rovers.forEach(rover -> occupiedCells.add(rover.position()));

        int turns = missions.stream().mapToInt(mission -> mission.instructions().size()).max().orElse(0);
        for (int turn = 0; turn < turns; turn++) {
            Set<Integer> blockedRovers = new HashSet<>();
            for (int i = 0; i < missions.size(); i++) {
                List<Instruction> instructions = missions.get(i).instructions();
                if (turn >= instructions.size()) {
                    continue;
                }
                Rover rover = rovers.get(i);
                Optional<Rover> next = rover.step(instructions.get(turn), input.plateau(), occupiedCells);
                if (next.isPresent()) {
                    occupiedCells.remove(rover.position());
                    occupiedCells.add(next.get().position());
                    rovers.set(i, next.get());
                } else {
                    blockedMoves[i]++;
                    blockedRovers.add(i);
                }
            }
            listener.onTurn(new Turn(turn + 1, rovers, blockedRovers));
        }
        return MissionResult.of(rovers, blockedMoves);
    }
}
