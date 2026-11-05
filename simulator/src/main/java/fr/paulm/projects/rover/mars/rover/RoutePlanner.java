package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

// Finds the fastest way for a rover to reach a cell, on the orbital map, as mission planners do: an A* search
// over the states of the rover (its cell and its direction, since turning takes time). From a state, the rover can
// turn left or right, or move forward when the terrain allows it. It never drives backwards over long distances,
// like real rovers. The cost of a step is the energy it takes, counted in time at the power of a drive on flat
// ground: a turn costs its time, a move its drive time times the extra power of climbing (see
// PowerSystem.slopeFactor), so a climb costs twice, slower and more power hungry.
// The estimate of the cost left is the distance as the crow flies on the sphere, at the speed on flat ground: it is
// never more than the real cost, so the route found is the cheapest one in energy. The search gives up beyond a number of
// states explored, so that an unreachable or very far goal does not run for ever.
public final class RoutePlanner {

    private static final int MAX_STATES = 500_000;

    private final Planet planet;
    private final ReliefTerrain terrain;
    private final Mobility mobility;

    public RoutePlanner(Planet planet, Mobility mobility) {
        this.planet = planet;
        this.terrain = new ReliefTerrain(planet, mobility);
        this.mobility = mobility;
    }

    // A state of the search, with the time to reach it and the estimated total time through it
    private record Node(Rover state, long millis, long estimate) {
    }

    // The instructions that bring the rover from its state into the cell of the goal, the fastest way;
    // empty when no route exists (or none within the limit of the search).
    public Optional<List<Instruction>> route(Rover start, Coordinates goal) {
        Position target = planet.cellAt(goal);
        Coordinates goalCenter = planet.centerOf(target);
        Map<Rover, Long> best = new HashMap<>();
        Map<Rover, Rover> cameFrom = new HashMap<>();
        Map<Rover, Instruction> howReached = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Long.compare(a.estimate(), b.estimate()));
        best.put(start, 0L);
        open.add(new Node(start, 0, estimate(start.position(), goalCenter)));
        while (!open.isEmpty() && best.size() < MAX_STATES) {
            Node node = open.poll();
            Rover state = node.state();
            if (node.millis() > best.get(state)) {
                continue;
            }
            if (state.position().equals(target)) {
                return Optional.of(instructions(state, cameFrom, howReached));
            }
            for (Instruction instruction : List.of(Instruction.MOVE, Instruction.LEFT, Instruction.RIGHT)) {
                Rover next = state.apply(instruction, planet.grid());
                Optional<Duration> cost = cost(state, next);
                if (cost.isEmpty()) {
                    continue;
                }
                long millis = node.millis() + cost.get().toMillis();
                if (millis < best.getOrDefault(next, Long.MAX_VALUE)) {
                    best.put(next, millis);
                    cameFrom.put(next, state);
                    howReached.put(next, instruction);
                    open.add(new Node(next, millis, millis + estimate(next.position(), goalCenter)));
                }
            }
        }
        return Optional.empty();
    }

    // The time of a step, or nothing if the terrain forbids it.
    private Optional<Duration> cost(Rover from, Rover to) {
        if (from.position().equals(to.position())) {
            return Optional.of(mobility.turnTime());
        }
        if (!terrain.canDrive(from.position(), to.position())) {
            return Optional.empty();
        }
        double factor = PowerSystem.slopeFactor(terrain.slope(from.position(), to.position()), mobility.maxSlopeDegrees());
        return Optional.of(Duration.ofMillis(Math.round(terrain.driveTime(from.position(), to.position()).toMillis() * factor)));
    }

    private long estimate(Position cell, Coordinates goalCenter) {
        double meters = planet.centerOf(cell).distanceTo(goalCenter, planet.radiusMeters());
        return Math.round(meters / mobility.flatSpeedMetersPerSecond() * 1_000);
    }

    private List<Instruction> instructions(Rover end, Map<Rover, Rover> cameFrom, Map<Rover, Instruction> howReached) {
        List<Instruction> instructions = new ArrayList<>();
        for (Rover state = end; cameFrom.containsKey(state); state = cameFrom.get(state)) {
            instructions.add(howReached.get(state));
        }
        Collections.reverse(instructions);
        return instructions;
    }
}
