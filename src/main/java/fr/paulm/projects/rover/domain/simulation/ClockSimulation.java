package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Obstacle;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

// The rovers act at the same time, on a common clock. Each instruction takes the time the terrain gives it
// (a drive, a turn on the spot, or nothing for a move the terrain blocks), and a rover starts its next
// instruction as soon as the previous one is over. An instruction takes effect when it ends: the simulation
// handles the ends of instructions in time order and sends each one to the listener, as a turn with its time.
// At the same time, instructions are handled by rank (all the first instructions, then all the second ones…),
// then in input order. Without durations, as on a plateau, this is exactly the step-by-step mode.
public final class ClockSimulation implements Simulation {

    // The end of the rank-th instruction of a rover (rank from 0), at the given time
    private record End(Duration time, int rank, int rover) {
    }

    @Override
    public List<MissionResult> run(SimulationInput input, TurnListener listener) {
        List<RoverMission> missions = input.missions();
        List<Rover> rovers = new ArrayList<>(missions.stream().map(RoverMission::rover).toList());
        MissionLog log = new MissionLog(missions.size(), input.terrain());
        Set<Position> occupiedCells = new HashSet<>();
        rovers.forEach(rover -> occupiedCells.add(rover.position()));
        // Empty when rovers can share cells: they never block each other
        Set<Position> roversCells = input.oneRoverPerCell() ? occupiedCells : Set.of();

        PriorityQueue<End> ends = new PriorityQueue<>(Comparator.comparing(End::time)
                .thenComparingInt(End::rank).thenComparingInt(End::rover));
        for (int i = 0; i < missions.size(); i++) {
            schedule(ends, input, i, 0, rovers.get(i), Duration.ZERO);
        }
        int turn = 0;
        while (!ends.isEmpty()) {
            End end = ends.poll();
            int i = end.rover();
            Rover rover = rovers.get(i);
            Rover next = rover.apply(missions.get(i).instructions().get(end.rank()), input.surface());
            Optional<Obstacle> obstacle = rover.obstacleTo(next, input.terrain(), roversCells);
            if (obstacle.isEmpty()) {
                occupiedCells.remove(rover.position());
                occupiedCells.add(next.position());
                log.done(i, rover, next);
                rovers.set(i, next);
            } else {
                log.blocked(i, obstacle.get());
            }
            listener.onTurn(new Turn(++turn, rovers, obstacle.isEmpty() ? Set.of() : Set.of(i), end.time()));
            schedule(ends, input, i, end.rank() + 1, rovers.get(i), end.time());
        }
        return log.results(rovers);
    }

    // Schedules the end of the rank-th instruction of a rover, which starts now, if it has one.
    // It lasts as long as the terrain says, from where the rover is now; a move the terrain blocks takes no time.
    private void schedule(PriorityQueue<End> ends, SimulationInput input, int rover, int rank, Rover state, Duration now) {
        List<Instruction> instructions = input.missions().get(rover).instructions();
        if (rank >= instructions.size()) {
            return;
        }
        Rover next = state.apply(instructions.get(rank), input.surface());
        ends.add(new End(now.plus(duration(input.terrain(), state, next)), rank, rover));
    }

    private Duration duration(Terrain terrain, Rover from, Rover to) {
        if (from.position().equals(to.position())) {
            return terrain.turnTime();
        }
        if (!terrain.canDrive(from.position(), to.position())) {
            return Duration.ZERO;
        }
        return terrain.driveTime(from.position(), to.position());
    }
}
