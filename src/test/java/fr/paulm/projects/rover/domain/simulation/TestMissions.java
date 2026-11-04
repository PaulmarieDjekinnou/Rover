package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;

// Short ways to write missions and their expected results in tests, with the letters of the text format.
public final class TestMissions {

    // A cliff between columns 1 and 2, all along the plateau: no rover can cross it, either way
    public static final Terrain CLIFF_EAST_OF_COLUMN_1 =
            (from, to) -> Math.min(from.x(), to.x()) != 1 || Math.max(from.x(), to.x()) != 2;

    private TestMissions() {
    }

    // mission(1, 2, N, "LM"): the rover in (1, 2) facing north, with the instructions LEFT, MOVE.
    public static RoverMission mission(int x, int y, Direction direction, String instructions) {
        return new RoverMission(new Rover(new Position(x, y), direction), TestInstructions.of(instructions));
    }

    // result(1, 3, N, 2): the rover ends in (1, 3) facing north, after 2 blocked moves.
    public static MissionResult result(int x, int y, Direction direction, int blockedMoves) {
        return new MissionResult(new Rover(new Position(x, y), direction), blockedMoves);
    }

    // result(1, 3, N, 0, 2): the rover ends in (1, 3) facing north, after 2 moves blocked by the terrain.
    public static MissionResult result(int x, int y, Direction direction, int blockedByRovers, int blockedByTerrain) {
        return new MissionResult(new Rover(new Position(x, y), direction), blockedByRovers, blockedByTerrain);
    }
}
