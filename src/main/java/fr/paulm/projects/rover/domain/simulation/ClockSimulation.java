package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Obstacle;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

// The rovers act at the same time, on a common clock. Each instruction takes the time the terrain gives it
// (a drive, a turn on the spot, or nothing for a blocked move), and a rover starts its next instruction as soon
// as the previous one is over.
// An instruction is decided when it starts: blocked or not, from where the rovers are and where they are going.
// A move then holds both cells until it ends: the rover still occupies the cell it leaves, and the cell it goes to
// is reserved, so that no other rover drives into it meanwhile. The move takes effect when it ends, and each end is
// sent to the listener, as a turn with its time.
// Starts and ends are handled in time order, then by rank (all the first instructions, then all the second ones…),
// then in input order, an end before a start. Without durations, as on a plateau, this is exactly the step-by-step
// mode: each rover sees the moves of the rovers before it in the same round.
// The pace may make a rover wait before it acts (to recharge, for instance), or stop it for good: a rover that
// stops leaves its cell to nobody else, and its other instructions undone.
public final class ClockSimulation implements Simulation {

    private final Pace pace;

    public ClockSimulation() {
        this(Pace.IMMEDIATE);
    }

    public ClockSimulation(Pace pace) {
        this.pace = pace;
    }

    // The start or the end of the rank-th instruction of a rover (rank from 0), at the given time
    private record Event(Duration time, int rank, int rover, boolean start) {
    }

    // The instruction a rover is carrying out: the state it will be in at the end, or what blocks it
    private record Decision(Rover next, Optional<Obstacle> obstacle) {
    }

    @Override
    public List<MissionResult> run(SimulationInput input, TurnListener listener) {
        List<RoverMission> missions = input.missions();
        List<Rover> rovers = new ArrayList<>(missions.stream().map(RoverMission::rover).toList());
        MissionLog log = new MissionLog(missions.size(), input.terrain());
        // How many rovers hold each cell: the rovers on it, and the rovers driving into it. A cell no rover holds
        // is removed, so the keys are the cells held
        Map<Position, Integer> heldCells = new HashMap<>();
        rovers.forEach(rover -> heldCells.merge(rover.position(), 1, Integer::sum));
        Map<Integer, Decision> decisions = new HashMap<>();

        PriorityQueue<Event> events = new PriorityQueue<>(Comparator.comparing(Event::time)
                .thenComparingInt(Event::rank).thenComparingInt(Event::rover)
                .thenComparing(Event::start));
        for (int i = 0; i < missions.size(); i++) {
            if (!missions.get(i).instructions().isEmpty()) {
                events.add(new Event(Duration.ZERO, 0, i, true));
            }
        }
        int turn = 0;
        while (!events.isEmpty()) {
            Event event = events.poll();
            int i = event.rover();
            Rover rover = rovers.get(i);
            if (event.start()) {
                Decision decision = decide(input, rover, missions.get(i).instructions().get(event.rank()), heldCells);
                Duration busy = duration(input.terrain(), rover, decision);
                Optional<Duration> duration = decision.obstacle().isPresent()
                        ? Optional.of(Duration.ZERO) : pace.duration(i, rover, event.time(), busy);
                if (duration.isEmpty()) {
                    log.stopped(i);
                    continue;
                }
                log.waited(i, duration.get().minus(busy));
                if (decision.obstacle().isEmpty() && !decision.next().position().equals(rover.position())) {
                    heldCells.merge(decision.next().position(), 1, Integer::sum);
                }
                decisions.put(i, decision);
                events.add(new Event(event.time().plus(duration.get()), event.rank(), i, false));
                continue;
            }
            Decision decision = decisions.remove(i);
            if (decision.obstacle().isEmpty()) {
                if (!decision.next().position().equals(rover.position())) {
                    heldCells.merge(rover.position(), -1, (held, left) -> held + left == 0 ? null : held + left);
                }
                log.done(i, rover, decision.next());
                rovers.set(i, decision.next());
            } else {
                log.blocked(i, decision.obstacle().get());
            }
            listener.onTurn(new Turn(++turn, rovers, decision.obstacle().isEmpty() ? Set.of() : Set.of(i), event.time()));
            if (event.rank() + 1 < missions.get(i).instructions().size()) {
                events.add(new Event(event.time(), event.rank() + 1, i, true));
            }
        }
        return log.results(rovers);
    }

    // What the instruction leads to, and what blocks it: the terrain, or a cell held by another rover when cells
    // hold one rover at most.
    private Decision decide(SimulationInput input, Rover rover, Instruction instruction, Map<Position, Integer> heldCells) {
        Rover next = rover.apply(instruction, input.surface());
        Set<Position> held = input.oneRoverPerCell() ? heldCells.keySet() : Set.of();
        return new Decision(next, rover.obstacleTo(next, input.terrain(), held));
    }

    private Duration duration(Terrain terrain, Rover rover, Decision decision) {
        if (decision.obstacle().isPresent()) {
            return Duration.ZERO;
        }
        if (decision.next().position().equals(rover.position())) {
            return terrain.turnTime();
        }
        return terrain.driveTime(rover.position(), decision.next().position());
    }
}
