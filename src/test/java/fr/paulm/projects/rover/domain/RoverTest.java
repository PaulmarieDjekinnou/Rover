package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.LogCapture;
import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static fr.paulm.projects.rover.domain.enums.Direction.E;
import static fr.paulm.projects.rover.domain.enums.Direction.N;
import static fr.paulm.projects.rover.domain.enums.Direction.S;
import static fr.paulm.projects.rover.domain.enums.Direction.W;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoverTest {

    private final Plateau plateau = new Plateau(5, 5);

    // Runs the instructions on the 5x5 plateau, with no other rover.
    private Rover run(Rover rover, String instructions) {
        return rover.execute(TestInstructions.of(instructions), plateau, Set.of());
    }

    // Examples from the problem statement
    @Test
    void firstSampleRover() {
        assertEquals(new Rover(new Position(1, 3), N), run(new Rover(new Position(1, 2), N), "LMLMLMLMM"));
    }

    @Test
    void secondSampleRover() {
        assertEquals(new Rover(new Position(5, 1), E), run(new Rover(new Position(3, 3), E), "MMRMMRMRRM"));
    }

    @Test
    void toStringFormatsPositionAndHeading() {
        assertEquals("2 4 W", new Rover(new Position(2, 4), W).toString());
    }

    @Test
    void emptyInstructionsLeaveRoverUnchanged() {
        assertEquals(new Rover(new Position(1, 1), S), run(new Rover(new Position(1, 1), S), ""));
    }

    @ParameterizedTest
    @CsvSource({"N, 2, 3", "E, 3, 2", "S, 2, 1", "W, 1, 2"})
    void moveGoesOneStepInEachDirection(Direction direction, int expectedX, int expectedY) {
        assertEquals(new Rover(new Position(expectedX, expectedY), direction), run(new Rover(new Position(2, 2), direction), "M"));
    }

    @Test
    void turnsDoNotChangePosition() {
        assertEquals(new Rover(new Position(2, 2), N), run(new Rover(new Position(2, 2), N), "LLLLRRRR"));
        assertEquals(new Rover(new Position(2, 2), S), run(new Rover(new Position(2, 2), N), "RR"));
        assertEquals(new Rover(new Position(2, 2), W), run(new Rover(new Position(2, 2), N), "L"));
    }

    @Test
    void roundTripReturnsToStart() {
        assertEquals(new Rover(new Position(2, 2), N), run(new Rover(new Position(2, 2), N), "MMRRMMRR"));
    }

    // Moves that would leave the plateau are ignored
    @ParameterizedTest
    @CsvSource({"0, 5, N", "5, 0, E", "0, 0, S", "0, 0, W"})
    void moveOffTheEdgeIsIgnored(int x, int y, Direction direction) {
        Rover start = new Rover(new Position(x, y), direction);
        assertEquals(start, run(start, "M"));
    }

    @Test
    void moveOffTheEdgeIsReportedAsAWarning() {
        LogCapture logs = LogCapture.start(Rover.class);
        try {
            run(new Rover(new Position(0, 5), N), "M");
            assertEquals(List.of("Rover starting at \"0 5 N\": 1 move(s) ignored because they would leave the plateau."),
                    logs.messages());
        } finally {
            logs.stop();
        }
    }

    @Test
    void severalIgnoredMovesAreReportedInASingleWarning() {
        LogCapture logs = LogCapture.start(Rover.class);
        try {
            // Reaches the north edge after 5 steps: the last 3 moves are ignored
            run(new Rover(new Position(0, 0), N), "MMMMMMMM");
            assertEquals(List.of("Rover starting at \"0 0 N\": 3 move(s) ignored because they would leave the plateau."),
                    logs.messages());
        } finally {
            logs.stop();
        }
    }

    @Test
    void noWarningWhenNoMoveIsIgnored() {
        LogCapture logs = LogCapture.start(Rover.class);
        try {
            run(new Rover(new Position(1, 2), N), "LMLMLMLMM");
            assertEquals(List.of(), logs.messages());
        } finally {
            logs.stop();
        }
    }

    @Test
    void roverStopsAtEdgeAndContinuesAfterwards() {
        // Blocked to the north after 5 steps, then turns and moves east
        assertEquals(new Rover(new Position(2, 5), E), run(new Rover(new Position(0, 0), N), "MMMMMMMMRMM"));
    }

    @Test
    void roverOnSingleCellPlateauNeverMoves() {
        Rover rover = new Rover(new Position(0, 0), N).execute(TestInstructions.of("MRMRMRMLM"), new Plateau(0, 0), Set.of());
        assertEquals(new Rover(new Position(0, 0), S), rover);
    }

    // Collisions: a move onto a cell occupied by another rover is ignored
    @Test
    void moveOntoAnOccupiedCellIsIgnored() {
        Rover rover = new Rover(new Position(2, 2), N)
                .execute(TestInstructions.of("M"), plateau, Set.of(new Position(2, 3)));
        assertEquals(new Rover(new Position(2, 2), N), rover);
    }

    @Test
    void roverGoesAroundAnOccupiedCell() {
        // Blocked to the north by a rover in (2, 3), then goes around it by the east
        Rover rover = new Rover(new Position(2, 2), N)
                .execute(TestInstructions.of("MRMLMM"), plateau, Set.of(new Position(2, 3)));
        assertEquals(new Rover(new Position(3, 4), N), rover);
    }

    @Test
    void movesIntoAnotherRoverAreReportedSeparatelyFromMovesOffThePlateau() {
        LogCapture logs = LogCapture.start(Rover.class);
        try {
            // From (0, 4) to the north: blocked once by the edge after one step, then twice by the rover in (1, 5)
            new Rover(new Position(0, 4), N)
                    .execute(TestInstructions.of("MMRMM"), plateau, Set.of(new Position(1, 5)));
            assertEquals(List.of(
                    "Rover starting at \"0 4 N\": 1 move(s) ignored because they would leave the plateau.",
                    "Rover starting at \"0 4 N\": 2 move(s) ignored because the cell is occupied by another rover."),
                    logs.messages());
        } finally {
            logs.stop();
        }
    }

    @Test
    void executeReturnsANewStateAndLeavesTheOriginalUnchanged() {
        Rover roverStart = new Rover(new Position(1, 2), N);
        Rover roverEnd = run(roverStart, "RMM");
        assertEquals(new Rover(new Position(1, 2), N), roverStart);
        assertEquals(new Rover(new Position(3, 2), E), roverEnd);
    }
}
