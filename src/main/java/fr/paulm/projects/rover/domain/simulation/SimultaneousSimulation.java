package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The rovers move turn by turn, all at the same time: in each turn, every rover executes its next instruction,
// and the order of the rovers in the input gives no priority. A move is blocked when:
// - several rovers want to move onto the same cell;
// - two rovers want to swap their cells by moving in opposite directions (they would cross each other).
//   On a plateau two cells wide, two rovers can also swap their cells by moving the same way around
//   the planet: they do not cross, so both move;
// - the cell is occupied by a rover that does not leave it during this turn.
// A rover can follow another one that leaves its cell in the same turn.
// A rover that has no instruction left stays on its cell until the end. Each turn is sent to the listener.
public final class SimultaneousSimulation implements Simulation {

    @Override
    public List<MissionResult> run(SimulationInput input, TurnListener listener) {
        List<RoverMission> missions = input.missions();
        List<Rover> rovers = new ArrayList<>(missions.stream().map(RoverMission::rover).toList());
        int[] blockedMoves = new int[missions.size()];

        int turns = missions.stream().mapToInt(mission -> mission.instructions().size()).max().orElse(0);
        for (int turn = 0; turn < turns; turn++) {
            List<Rover> wanted = new ArrayList<>();
            List<Direction> travels = new ArrayList<>();
            for (int i = 0; i < missions.size(); i++) {
                List<Instruction> instructions = missions.get(i).instructions();
                Rover rover = rovers.get(i);
                boolean finished = turn >= instructions.size();
                wanted.add(finished ? rover : rover.apply(instructions.get(turn), input.surface()));
                // The way the rover moves: backwards for BACK, forward otherwise (only used if it changes cell)
                boolean back = !finished && instructions.get(turn) == Instruction.BACK;
                travels.add(back ? rover.direction().opposite() : rover.direction());
            }
            boolean[] blocked = blockedMovesOfTurn(rovers, wanted, travels);
            Set<Integer> blockedRovers = new HashSet<>();
            for (int i = 0; i < rovers.size(); i++) {
                if (blocked[i]) {
                    blockedMoves[i]++;
                    blockedRovers.add(i);
                } else {
                    rovers.set(i, wanted.get(i));
                }
            }
            listener.onTurn(new Turn(turn + 1, rovers, blockedRovers));
        }
        return MissionResult.of(rovers, blockedMoves);
    }

    // Which rovers cannot make the move they want in this turn. A rover that turns or stays never is.
    private boolean[] blockedMovesOfTurn(List<Rover> rovers, List<Rover> wanted, List<Direction> travels) {
        int count = rovers.size();
        Map<Position, Integer> roverAt = new HashMap<>();
        Map<Position, Integer> roversWanting = new HashMap<>();
        for (int i = 0; i < count; i++) {
            roverAt.put(rovers.get(i).position(), i);
            roversWanting.merge(wanted.get(i).position(), 1, Integer::sum);
        }

        boolean[] blocked = new boolean[count];
        for (int i = 0; i < count; i++) {
            Position target = wanted.get(i).position();
            if (!moves(i, rovers, wanted)) {
                continue;
            }
            Integer other = roverAt.get(target);
            boolean sameTarget = roversWanting.get(target) > 1;
            boolean crossing = other != null && wanted.get(other).position().equals(rovers.get(i).position())
                    && travels.get(other) == travels.get(i).opposite();
            blocked[i] = sameTarget || crossing;
        }

        // A rover that stays (or is blocked) blocks the rover that wanted its cell, which may block another one:
        // repeat until nothing changes.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < count; i++) {
                if (!moves(i, rovers, wanted) || blocked[i]) {
                    continue;
                }
                Integer other = roverAt.get(wanted.get(i).position());
                if (other != null && (!moves(other, rovers, wanted) || blocked[other])) {
                    blocked[i] = true;
                    changed = true;
                }
            }
        }
        return blocked;
    }

    private boolean moves(int i, List<Rover> rovers, List<Rover> wanted) {
        return !wanted.get(i).position().equals(rovers.get(i).position());
    }
}
