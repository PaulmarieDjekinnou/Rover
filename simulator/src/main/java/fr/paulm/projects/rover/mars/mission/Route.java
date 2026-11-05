package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Instruction;

import java.util.List;

// What a rover carries out once on the ground: its orders, or the instructions planned to reach its waypoints,
// with how many of them a route was found to (planning stops at the first unreachable waypoint).
public record Route(List<Instruction> instructions, int reachedWaypoints, int waypoints) {

    public Route {
        instructions = List.copyOf(instructions);
    }

    // A rover that follows orders: no waypoint.
    public static Route of(List<Instruction> orders) {
        return new Route(orders, 0, 0);
    }

    public boolean complete() {
        return reachedWaypoints == waypoints;
    }
}
