package fr.paulm.projects.rover.domain;

import java.util.ArrayList;
import java.util.List;

// How a rover's mission ended: its final state, and how many moves were blocked by another rover.
public record MissionResult(Rover finalRover, int blockedMoves) {

    // The results of all the missions, from the final state of each rover and its number of blocked moves,
    // in input order.
    static List<MissionResult> of(List<Rover> finalRovers, int[] blockedMoves) {
        List<MissionResult> results = new ArrayList<>();
        for (int i = 0; i < finalRovers.size(); i++) {
            results.add(new MissionResult(finalRovers.get(i), blockedMoves[i]));
        }
        return results;
    }
}
