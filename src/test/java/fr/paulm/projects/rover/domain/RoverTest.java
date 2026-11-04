package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.TestInstructions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;
import java.util.Set;

import static fr.paulm.projects.rover.domain.Direction.E;
import static fr.paulm.projects.rover.domain.Direction.N;
import static fr.paulm.projects.rover.domain.Direction.S;
import static fr.paulm.projects.rover.domain.Direction.W;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoverTest {

    private final Plateau plateau = new Plateau(5, 5);

    // Applies the instructions one after the other on the given plateau, with no other rover.
    private static Rover run(Rover rover, String instructions, Plateau plateau) {
        Rover current = rover;
        for (Instruction instruction : TestInstructions.of(instructions)) {
            current = current.apply(instruction, plateau);
        }
        return current;
    }

    private Rover run(Rover rover, String instructions) {
        return run(rover, instructions, plateau);
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

    @ParameterizedTest
    @CsvSource({"N, 2, 1", "E, 1, 2", "S, 2, 3", "W, 3, 2"})
    void backGoesOneStepBackwardsAndKeepsTheDirection(Direction direction, int expectedX, int expectedY) {
        assertEquals(new Rover(new Position(expectedX, expectedY), direction), run(new Rover(new Position(2, 2), direction), "B"));
    }

    @Test
    void backCancelsMove() {
        assertEquals(new Rover(new Position(2, 2), E), run(new Rover(new Position(2, 2), E), "MMBB"));
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
    void backOffTheEdgeWrapsAround() {
        assertEquals(new Rover(new Position(0, 5), N), run(new Rover(new Position(0, 0), N), "B"));
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
        assertEquals(new Rover(new Position(0, 0), S), run(new Rover(new Position(0, 0), N), "MRMRMRMLM", new Plateau(0, 0)));
    }

    @Test
    void applyReturnsANewStateAndLeavesTheOriginalUnchanged() {
        Rover roverStart = new Rover(new Position(1, 2), N);
        Rover roverEnd = run(roverStart, "RMM");
        assertEquals(new Rover(new Position(1, 2), N), roverStart);
        assertEquals(new Rover(new Position(3, 2), E), roverEnd);
    }

    // Collisions: step refuses a move onto a cell occupied by another rover
    @Test
    void stepMovesOntoAFreeCell() {
        assertEquals(Optional.of(new Rover(new Position(2, 3), N)),
                new Rover(new Position(2, 2), N).step(Instruction.MOVE, plateau, Set.of(new Position(4, 4))));
    }

    @Test
    void stepOntoAnOccupiedCellIsBlocked() {
        assertEquals(Optional.empty(), new Rover(new Position(2, 2), N).step(Instruction.MOVE, plateau, Set.of(new Position(2, 3))));
    }

    @Test
    void stepBackOntoAnOccupiedCellIsBlocked() {
        assertEquals(Optional.empty(), new Rover(new Position(2, 2), N).step(Instruction.BACK, plateau, Set.of(new Position(2, 1))));
    }

    @Test
    void stepIsBlockedByARoverOnTheOtherSideOfTheEdge() {
        // Leaving (0, 5) to the north leads to (0, 0), which is occupied
        assertEquals(Optional.empty(), new Rover(new Position(0, 5), N).step(Instruction.MOVE, plateau, Set.of(new Position(0, 0))));
    }

    @Test
    void turnIsNeverBlocked() {
        Set<Position> allAround = Set.of(new Position(2, 3), new Position(3, 2), new Position(2, 1), new Position(1, 2));
        assertEquals(Optional.of(new Rover(new Position(2, 2), W)),
                new Rover(new Position(2, 2), N).step(Instruction.LEFT, plateau, allAround));
    }

    @Test
    void stepIgnoresTheRoverOwnCell() {
        Set<Position> ownCell = Set.of(new Position(2, 2));
        assertEquals(Optional.of(new Rover(new Position(2, 2), E)),
                new Rover(new Position(2, 2), N).step(Instruction.RIGHT, plateau, ownCell));
        // On a single-cell plateau, a move comes back to the same cell: it is not blocked either
        assertEquals(Optional.of(new Rover(new Position(0, 0), N)),
                new Rover(new Position(0, 0), N).step(Instruction.MOVE, new Plateau(0, 0), Set.of(new Position(0, 0))));
    }
}
