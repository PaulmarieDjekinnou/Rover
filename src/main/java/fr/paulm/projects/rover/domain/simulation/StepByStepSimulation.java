package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Obstacle;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;

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
        MissionLog log = new MissionLog(missions.size());
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
                Rover next = rover.apply(instructions.get(turn), input.surface());
                Optional<Obstacle> obstacle = rover.obstacleTo(next, input.terrain(), occupiedCells);
                if (obstacle.isEmpty()) {
                    occupiedCells.remove(rover.position());
                    occupiedCells.add(next.position());
                    rovers.set(i, next);
                } else {
                    log.blocked(i, obstacle.get());
                    blockedRovers.add(i);
                }
            }
            listener.onTurn(new Turn(turn + 1, rovers, blockedRovers));
        }
        return log.results(rovers);
    }
}
