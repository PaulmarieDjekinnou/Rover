package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.Pilot;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.rover.RoutePlanner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The autonomous navigation of a rover with waypoints, as Perseverance's AutoNav: it follows a route planned on the
// orbital map, which does not show the rock fields. Before each move, its cameras look at the cell ahead: a rock field
// there is remembered, and the rest of the way to the waypoint is planned again, around the rock fields it knows.
// A waypoint no route reaches (with what it knows) is skipped.
public final class AutoNav implements Pilot {
    private final Planet planet;
    private final RoutePlanner planner;
    private final Deque<Coordinates> waypoints;
    private final Deque<Instruction> plan = new ArrayDeque<>();
    private final Set<Position> knownRocks = new HashSet<>();
    private Coordinates goal;
    private int replans;
    private int missed;

    public AutoNav(Planet planet, RoutePlanner planner, List<Coordinates> waypoints) {
        this.planet = planet;
        this.planner = planner;
        this.waypoints = new ArrayDeque<>(waypoints);
    }

    @Override
    public Optional<Instruction> next(Rover state) {
        while (plan.isEmpty()) {
            if (waypoints.isEmpty()) {
                return Optional.empty();
            }
            goal = waypoints.removeFirst();
            if (!planTo(state, goal)) {
                missed++;
            }
        }
        Rover ahead = state.apply(plan.peekFirst(), planet.grid());
        if (!ahead.position().equals(state.position()) && planet.rocky(ahead.position())) {
            // A rock field seen ahead: plan the rest of the way to this waypoint again, around the ones it knows
            knownRocks.add(ahead.position());
            replans++;
            plan.clear();
            if (!planTo(state, goal)) {
                missed++;
            }
            return next(state);
        }
        return Optional.of(plan.pollFirst());
    }

    // The rock fields the rover has found on its way, and how many times it planned around them.
    public int rockFieldsFound() {
        return knownRocks.size();
    }

    public int replans() {
        return replans;
    }

    // The waypoints no route reached.
    public int missedWaypoints() {
        return missed;
    }

    private boolean planTo(Rover state, Coordinates goal) {
        Optional<List<Instruction>> route = planner.route(state, goal, knownRocks);
        route.ifPresent(plan::addAll);
        return route.isPresent();
    }
}
