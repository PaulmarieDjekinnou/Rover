package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Obstacle;
import fr.paulm.projects.rover.domain.Rover;

import java.util.ArrayList;
import java.util.List;

// What happens to each rover during a simulation, written down as it goes: its moves blocked by another rover
// or by the terrain. Rovers are identified by their index in input order. Gives the results at the end.
final class MissionLog {
    private final int[] blockedByRovers;
    private final int[] blockedByTerrain;

    MissionLog(int rovers) {
        blockedByRovers = new int[rovers];
        blockedByTerrain = new int[rovers];
    }

    void blocked(int rover, Obstacle obstacle) {
        switch (obstacle) {
            case ROVER -> blockedByRovers[rover]++;
            case TERRAIN -> blockedByTerrain[rover]++;
        }
    }

    // The result of each mission, in input order, from the final state of each rover.
    List<MissionResult> results(List<Rover> finalRovers) {
        List<MissionResult> results = new ArrayList<>();
        for (int i = 0; i < finalRovers.size(); i++) {
            results.add(new MissionResult(finalRovers.get(i), blockedByRovers[i], blockedByTerrain[i]));
        }
        return results;
    }
}
