package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The rovers move turn by turn, all at the same time: in each turn, every rover executes its next instruction,
// and the order of the rovers in the input gives no priority. A move is blocked when:
// - several rovers want to move onto the same cell;
// - two rovers want to swap their cells (they would cross each other);
// - the cell is occupied by a rover that does not leave it during this turn.
// A rover can follow another one that leaves its cell in the same turn.
// A rover that has no instruction left stays on its cell until the end.
public final class SimultaneousSimulation implements Simulation {

    @Override
    public List<MissionResult> run(SimulationInput input) {
        List<RoverMission> missions = input.missions();
        List<Rover> rovers = new ArrayList<>(missions.stream().map(RoverMission::rover).toList());
        int[] blockedMoves = new int[missions.size()];

        int turns = missions.stream().mapToInt(mission -> mission.instructions().size()).max().orElse(0);
        for (int turn = 0; turn < turns; turn++) {
            List<Rover> wanted = new ArrayList<>();
            for (int i = 0; i < missions.size(); i++) {
                List<Instruction> instructions = missions.get(i).instructions();
                Rover rover = rovers.get(i);
                wanted.add(turn < instructions.size() ? rover.apply(instructions.get(turn), input.plateau()) : rover);
            }
            boolean[] blocked = blockedMovesOfTurn(rovers, wanted);
            for (int i = 0; i < rovers.size(); i++) {
                if (blocked[i]) {
                    blockedMoves[i]++;
                } else {
                    rovers.set(i, wanted.get(i));
                }
            }
        }

        List<MissionResult> results = new ArrayList<>();
        for (int i = 0; i < rovers.size(); i++) {
            results.add(new MissionResult(rovers.get(i), blockedMoves[i]));
        }
        return results;
    }

    // Which rovers cannot make the move they want in this turn. A rover that turns or stays never is.
    private boolean[] blockedMovesOfTurn(List<Rover> rovers, List<Rover> wanted) {
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
            boolean swap = other != null && wanted.get(other).position().equals(rovers.get(i).position());
            blocked[i] = sameTarget || swap;
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
