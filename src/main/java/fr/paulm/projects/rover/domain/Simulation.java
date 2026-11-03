package fr.paulm.projects.rover.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Runs the missions. It only computes results: displaying them is up to the caller,
// so the domain has no side effect.
public final class Simulation {
    private final SimulationInput simulationInput;

    public Simulation(SimulationInput simulationInput) {
        this.simulationInput = simulationInput;
    }

    // Moves each rover according to its instructions, in input order, and returns how each mission ended.
    // All rovers are on the plateau from the start: a rover cannot move onto the cell of a rover
    // that has finished (its final position) or that has not started yet (its starting position).
    public List<MissionResult> run() {
        Set<Position> occupiedCells = new HashSet<>();
        simulationInput.missions().forEach(mission -> occupiedCells.add(mission.rover().position()));

        List<MissionResult> results = new ArrayList<>();
        for (RoverMission mission : simulationInput.missions()) {
            Rover start = mission.rover();
            occupiedCells.remove(start.position());
            MissionResult result = start.execute(mission.instructions(), simulationInput.plateau(), Set.copyOf(occupiedCells));
            occupiedCells.add(result.finalRover().position());
            results.add(result);
        }
        return results;
    }
}
