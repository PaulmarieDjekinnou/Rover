package fr.paulm.projects.rover.domain;

import java.util.List;

// Builds the result of each mission by following the turns of a simulation, one at a time.
// A turn holds the state of every rover, so the last one gives the final positions,
// and the blocked rovers of each turn give the number of blocked moves.
public final class ResultTracker {
    private List<Rover> rovers;
    private final int[] blockedMoves;

    public ResultTracker(SimulationInput input) {
        rovers = input.missions().stream().map(RoverMission::rover).toList();
        blockedMoves = new int[rovers.size()];
    }

    public void record(Turn turn) {
        rovers = turn.rovers();
        turn.blockedRovers().forEach(rover -> blockedMoves[rover]++);
    }

    public List<MissionResult> results() {
        return MissionResult.of(rovers, blockedMoves);
    }
}
