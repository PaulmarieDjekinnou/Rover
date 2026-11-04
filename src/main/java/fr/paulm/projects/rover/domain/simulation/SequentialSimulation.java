package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Obstacle;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The rovers move one after the other: each rover executes all its instructions before the next one starts.
// Each instruction executed is a turn, sent to the listener.
public final class SequentialSimulation implements Simulation {

    // All rovers are on the surface from the start: a rover cannot move onto the cell of a rover
    // that has finished (its final position) or that has not started yet (its starting position).
    @Override
    public List<MissionResult> run(SimulationInput input, TurnListener listener) {
        List<RoverMission> missions = input.missions();
        List<Rover> rovers = new ArrayList<>(missions.stream().map(RoverMission::rover).toList());
        MissionLog log = new MissionLog(missions.size(), input.terrain());
        int turn = 0;
        Set<Position> occupiedCells = new HashSet<>();
        rovers.forEach(rover -> occupiedCells.add(rover.position()));

        // A read-only view, not a copy: the rover cannot change it, the simulation updates it after each move.
        // Empty when rovers can share cells: they never block each other
        Set<Position> roversCells = input.oneRoverPerCell() ? Collections.unmodifiableSet(occupiedCells) : Set.of();
        for (int i = 0; i < missions.size(); i++) {
            for (Instruction instruction : missions.get(i).instructions()) {
                Rover rover = rovers.get(i);
                Rover next = rover.apply(instruction, input.surface());
                Optional<Obstacle> obstacle = rover.obstacleTo(next, input.terrain(), roversCells);
                if (obstacle.isEmpty()) {
                    occupiedCells.remove(rover.position());
                    occupiedCells.add(next.position());
                    log.done(i, rover, next);
                    rovers.set(i, next);
                } else {
                    log.blocked(i, obstacle.get());
                }
                listener.onTurn(new Turn(++turn, rovers, obstacle.isEmpty() ? Set.of() : Set.of(i)));
            }
        }
        return log.results(rovers);
    }
}
