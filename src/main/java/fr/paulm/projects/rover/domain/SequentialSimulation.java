package fr.paulm.projects.rover.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The rovers move one after the other: each rover executes all its instructions before the next one starts.
public final class SequentialSimulation implements Simulation {

    // All rovers are on the plateau from the start: a rover cannot move onto the cell of a rover
    // that has finished (its final position) or that has not started yet (its starting position).
    @Override
    public List<MissionResult> run(SimulationInput input) {
        Set<Position> occupiedCells = new HashSet<>();
        input.missions().forEach(mission -> occupiedCells.add(mission.rover().position()));

        // A read-only view, not a copy: the rover cannot change the cells of the others,
        // and the set is only modified between two missions.
        Set<Position> otherRoversCells = Collections.unmodifiableSet(occupiedCells);
        List<MissionResult> results = new ArrayList<>();
        for (RoverMission mission : input.missions()) {
            Rover start = mission.rover();
            occupiedCells.remove(start.position());
            MissionResult result = start.execute(mission.instructions(), input.plateau(), otherRoversCells);
            occupiedCells.add(result.finalRover().position());
            results.add(result);
        }
        return results;
    }
}
