package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The rovers move one after the other: each rover executes all its instructions before the next one starts.
// Each instruction executed is a turn, sent to the listener.
public final class SequentialSimulation implements Simulation {

    // All rovers are on the plateau from the start: a rover cannot move onto the cell of a rover
    // that has finished (its final position) or that has not started yet (its starting position).
    @Override
    public List<MissionResult> run(SimulationInput input, TurnListener listener) {
        List<RoverMission> missions = input.missions();
        List<Rover> rovers = new ArrayList<>(missions.stream().map(RoverMission::rover).toList());
        int[] blockedMoves = new int[missions.size()];
        int turn = 0;
        Set<Position> occupiedCells = new HashSet<>();
        rovers.forEach(rover -> occupiedCells.add(rover.position()));

        // A read-only view, not a copy: the rover cannot change it, the simulation updates it after each move
        Set<Position> roversCells = Collections.unmodifiableSet(occupiedCells);
        for (int i = 0; i < missions.size(); i++) {
            for (Instruction instruction : missions.get(i).instructions()) {
                Rover rover = rovers.get(i);
                Optional<Rover> next = rover.step(instruction, input.plateau(), roversCells);
                if (next.isPresent()) {
                    occupiedCells.remove(rover.position());
                    occupiedCells.add(next.get().position());
                    rovers.set(i, next.get());
                } else {
                    blockedMoves[i]++;
                }
                listener.onTurn(new Turn(++turn, rovers, next.isPresent() ? Set.of() : Set.of(i)));
            }
        }
        return MissionResult.of(rovers, blockedMoves);
    }
}
