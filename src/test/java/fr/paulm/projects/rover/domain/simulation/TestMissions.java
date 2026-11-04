package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;

// Short ways to write missions and their expected results in tests, with the letters of the text format.
public final class TestMissions {

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
}
