package fr.paulm.projects.rover.domain;

import java.util.ArrayList;
import java.util.List;

// What a simulation returns: how each mission ended, in input order, and the state of the rovers after each turn.
public record SimulationResult(List<MissionResult> missions, List<Turn> history) {

    public SimulationResult {
        missions = List.copyOf(missions);
        history = List.copyOf(history);
    }

    // Builds the result from the final state of each rover and its number of blocked moves, in input order.
    static SimulationResult of(List<Rover> finalRovers, int[] blockedMoves, List<Turn> history) {
        List<MissionResult> missions = new ArrayList<>();
        for (int i = 0; i < finalRovers.size(); i++) {
            missions.add(new MissionResult(finalRovers.get(i), blockedMoves[i]));
        }
        return new SimulationResult(missions, history);
    }
}
