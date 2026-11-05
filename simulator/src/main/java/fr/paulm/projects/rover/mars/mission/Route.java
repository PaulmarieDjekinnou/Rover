package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Instruction;

import java.util.List;

// What a rover carries out once on the ground: its orders, or the instructions planned to reach its waypoints,
// with the waypoints no route reaches (by index, from 0): those are skipped, and the rover goes on to the next ones.
public record Route(List<Instruction> instructions, int waypoints, List<Integer> missedWaypoints) {

    public Route {
        instructions = List.copyOf(instructions);
        missedWaypoints = List.copyOf(missedWaypoints);
    }

    // A rover that follows orders: no waypoint.
    public static Route of(List<Instruction> orders) {
        return new Route(orders, 0, List.of());
    }

    public int reachedWaypoints() {
        return waypoints - missedWaypoints.size();
    }

    public boolean complete() {
        return missedWaypoints.isEmpty();
    }
}
