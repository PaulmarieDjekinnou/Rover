package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Obstacle;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// What happens to each rover during a simulation, written down as it goes: its moves blocked by another rover
// or by the terrain, the distance it drove and the time it took. Rovers are identified by their index in input
// order. Gives the results at the end.
// Each rover has its own clock: the time of a rover is the sum of its own moves and turns. A blocked move takes
// no time.
final class MissionLog {
    private final Terrain terrain;
    private final int[] blockedByRovers;
    private final int[] blockedByTerrain;
    private final double[] distances;
    private final Duration[] times;

    MissionLog(int rovers, Terrain terrain) {
        this.terrain = terrain;
        blockedByRovers = new int[rovers];
        blockedByTerrain = new int[rovers];
        distances = new double[rovers];
        times = new Duration[rovers];
        Arrays.fill(times, Duration.ZERO);
    }

    void blocked(int rover, Obstacle obstacle) {
        switch (obstacle) {
            case ROVER -> blockedByRovers[rover]++;
            case TERRAIN -> blockedByTerrain[rover]++;
        }
    }

    // The rover went from one state to the next: a drive to another cell, or a turn on the spot.
    void done(int rover, Rover from, Rover to) {
        if (from.position().equals(to.position())) {
            times[rover] = times[rover].plus(terrain.turnTime());
        } else {
            distances[rover] += terrain.distance(from.position(), to.position());
            times[rover] = times[rover].plus(terrain.driveTime(from.position(), to.position()));
        }
    }

    // The result of each mission, in input order, from the final state of each rover.
    List<MissionResult> results(List<Rover> finalRovers) {
        List<MissionResult> results = new ArrayList<>();
        for (int i = 0; i < finalRovers.size(); i++) {
            results.add(new MissionResult(finalRovers.get(i), blockedByRovers[i], blockedByTerrain[i],
                    distances[i], times[i]));
        }
        return results;
    }
}
