package fr.paulm.projects.rover.domain;

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
        return rover.execute(TestInstructions.of(instructions), plateau, Set.of()).finalRover();
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

    // The plateau wraps around: a rover leaving one edge comes back on the opposite edge
    @ParameterizedTest
    @CsvSource({"0, 5, N, 0, 0", "5, 0, E, 0, 0", "0, 0, S, 0, 5", "0, 0, W, 5, 0"})
    void moveOffTheEdgeWrapsAround(int x, int y, Direction direction, int expectedX, int expectedY) {
        assertEquals(new Rover(new Position(expectedX, expectedY), direction), run(new Rover(new Position(x, y), direction), "M"));
    }

    @Test
    void goingAroundThePlanetComesBackToTheStart() {
        // 6 cells high: 6 moves to the north bring the rover back to its starting cell
        assertEquals(new Rover(new Position(0, 0), N), run(new Rover(new Position(0, 0), N), "MMMMMM"));
    }

    @Test
    void roverKeepsGoingAfterWrappingAround() {
        // 8 moves to the north on a 6-cell column end in (0, 2), then 2 moves to the east
        assertEquals(new Rover(new Position(2, 2), E), run(new Rover(new Position(0, 0), N), "MMMMMMMMRMM"));
    }

    @Test
    void roverOnSingleCellPlateauNeverMoves() {
        Rover rover = new Rover(new Position(0, 0), N).execute(TestInstructions.of("MRMRMRMLM"), new Plateau(0, 0), Set.of()).finalRover();
        assertEquals(new Rover(new Position(0, 0), S), rover);
    }

    // Collisions: a move onto a cell occupied by another rover is ignored
    @Test
    void moveOntoAnOccupiedCellIsIgnored() {
        Rover rover = new Rover(new Position(2, 2), N)
                .execute(TestInstructions.of("M"), plateau, Set.of(new Position(2, 3))).finalRover();
        assertEquals(new Rover(new Position(2, 2), N), rover);
    }

    @Test
    void roverGoesAroundAnOccupiedCell() {
        // Blocked to the north by a rover in (2, 3), then goes around it by the east
        Rover rover = new Rover(new Position(2, 2), N)
                .execute(TestInstructions.of("MRMLMM"), plateau, Set.of(new Position(2, 3))).finalRover();
        assertEquals(new Rover(new Position(3, 4), N), rover);
    }

    @Test
    void roverIsBlockedByARoverOnTheOtherSideOfTheEdge() {
        // Leaving (0, 5) to the north leads to (0, 0), which is occupied
        Rover rover = new Rover(new Position(0, 5), N)
                .execute(TestInstructions.of("M"), plateau, Set.of(new Position(0, 0))).finalRover();
        assertEquals(new Rover(new Position(0, 5), N), rover);
    }

    @Test
    void blockedMovesAreCounted() {
        MissionResult result = new Rover(new Position(2, 2), N)
                .execute(TestInstructions.of("MMRM"), plateau, Set.of(new Position(2, 3)));
        assertEquals(new MissionResult(new Rover(new Position(3, 2), E), 2), result);
    }

    @Test
    void noBlockedMoveWhenNothingIsInTheWay() {
        MissionResult result = new Rover(new Position(0, 5), N).execute(TestInstructions.of("MMLMLM"), plateau, Set.of());
        assertEquals(0, result.blockedMoves());
    }

    @Test
    void executeReturnsANewStateAndLeavesTheOriginalUnchanged() {
        Rover roverStart = new Rover(new Position(1, 2), N);
        Rover roverEnd = run(roverStart, "RMM");
        assertEquals(new Rover(new Position(1, 2), N), roverStart);
        assertEquals(new Rover(new Position(3, 2), E), roverEnd);
    }
}
