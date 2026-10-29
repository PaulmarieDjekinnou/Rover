package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.LogCapture;
import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static fr.paulm.projects.rover.domain.enums.Direction.E;
import static fr.paulm.projects.rover.domain.enums.Direction.N;
import static fr.paulm.projects.rover.domain.enums.Direction.S;
import static fr.paulm.projects.rover.domain.enums.Direction.W;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoverTest {

    private final Plateau plateau = new Plateau(5, 5);

    private Rover run(Rover rover, String instructions) {
        return rover.execute(TestInstructions.of(instructions), plateau);
    }

    // Examples from the problem statement
    @Test
    void firstSampleRover() {
        assertEquals(new Rover(1, 3, N), run(new Rover(1, 2, N), "LMLMLMLMM"));
    }

    @Test
    void secondSampleRover() {
        assertEquals(new Rover(5, 1, E), run(new Rover(3, 3, E), "MMRMMRMRRM"));
    }

    @Test
    void toStringFormatsPositionAndHeading() {
        assertEquals("2 4 W", new Rover(2, 4, W).toString());
    }

    @Test
    void emptyInstructionsLeaveRoverUnchanged() {
        assertEquals(new Rover(1, 1, S), run(new Rover(1, 1, S), ""));
    }

    @ParameterizedTest
    @CsvSource({"N, 2, 3", "E, 3, 2", "S, 2, 1", "W, 1, 2"})
    void moveGoesOneStepInEachDirection(Direction heading, int expectedX, int expectedY) {
        assertEquals(new Rover(expectedX, expectedY, heading), run(new Rover(2, 2, heading), "M"));
    }

    @Test
    void turnsDoNotChangePosition() {
        assertEquals(new Rover(2, 2, N), run(new Rover(2, 2, N), "LLLLRRRR"));
        assertEquals(new Rover(2, 2, S), run(new Rover(2, 2, N), "RR"));
        assertEquals(new Rover(2, 2, W), run(new Rover(2, 2, N), "L"));
    }

    @Test
    void roundTripReturnsToStart() {
        assertEquals(new Rover(2, 2, N), run(new Rover(2, 2, N), "MMRRMMRR"));
    }

    // Moves that would leave the plateau are ignored
    @ParameterizedTest
    @CsvSource({"0, 5, N", "5, 0, E", "0, 0, S", "0, 0, W"})
    void moveOffTheEdgeIsIgnored(int x, int y, Direction heading) {
        Rover start = new Rover(x, y, heading);
        assertEquals(start, run(start, "M"));
    }

    @Test
    void moveOffTheEdgeIsReportedAsAWarning() {
        LogCapture logs = LogCapture.start(Rover.class);
        try {
            run(new Rover(0, 5, N), "M");
            assertEquals(List.of("Rover at \"0 5 N\" cannot move to \"0 6\": move ignored."), logs.messages());
        } finally {
            logs.stop();
        }
    }

    @Test
    void roverStopsAtEdgeAndContinuesAfterwards() {
        // Blocked to the north after 5 steps, then turns and moves east
        assertEquals(new Rover(2, 5, E), run(new Rover(0, 0, N), "MMMMMMMMRMM"));
    }

    @Test
    void roverOnSingleCellPlateauNeverMoves() {
        Rover rover = new Rover(0, 0, N).execute(TestInstructions.of("MRMRMRMLM"), new Plateau(0, 0));
        assertEquals(new Rover(0, 0, S), rover);
    }

    @Test
    void executeReturnsANewStateAndLeavesTheOriginalUnchanged() {
        Rover roverStart = new Rover(1, 2, N);
        Rover roverEnd = run(roverStart, "RMM");
        assertEquals(new Rover(1, 2, N), roverStart);
        assertEquals(new Rover(3, 2, E), roverEnd);
    }
}
