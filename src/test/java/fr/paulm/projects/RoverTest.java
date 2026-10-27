package fr.paulm.projects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoverTest {

    private final Plateau plateau = new Plateau(5, 5);

    private String run(int x, int y, Direction heading, String instructions) {
        Rover rover = new Rover(x, y, heading);
        rover.execute(instructions, plateau);
        return rover.toString();
    }

    // Cas d'exemple de l'énoncé
    @Test
    void firstSampleRover() {
        assertEquals("1 3 N", run(1, 2, Direction.N, "LMLMLMLMM"));
    }

    @Test
    void secondSampleRover() {
        assertEquals("5 1 E", run(3, 3, Direction.E, "MMRMMRMRRM"));
    }

    @Test
    void toStringFormatsPositionAndHeading() {
        assertEquals("2 4 W", new Rover(2, 4, Direction.W).toString());
    }

    @Test
    void emptyInstructionsLeaveRoverUnchanged() {
        assertEquals("1 1 S", run(1, 1, Direction.S, ""));
    }

    @ParameterizedTest
    @CsvSource({"N, 2 3 N", "E, 3 2 E", "S, 2 1 S", "W, 1 2 W"})
    void moveGoesOneStepInEachDirection(Direction heading, String expected) {
        assertEquals(expected, run(2, 2, heading, "M"));
    }

    @Test
    void turnsDoNotChangePosition() {
        assertEquals("2 2 N", run(2, 2, Direction.N, "LLLLRRRR"));
        assertEquals("2 2 S", run(2, 2, Direction.N, "RR"));
        assertEquals("2 2 W", run(2, 2, Direction.N, "L"));
    }

    @Test
    void roundTripReturnsToStart() {
        assertEquals("2 2 N", run(2, 2, Direction.N, "MMRRMMRR"));
    }

    @Test
    void squarePathReturnsToStart() {
        assertEquals("0 0 N", run(0, 0, Direction.N, "MRMRMRMR"));
    }

    // Les mouvements qui sortiraient du plateau sont ignorés
    @ParameterizedTest
    @CsvSource({
            "0, 5, N, 0 5 N",
            "5, 0, E, 5 0 E",
            "0, 0, S, 0 0 S",
            "0, 0, W, 0 0 W",
    })
    void moveOffTheEdgeIsIgnored(int x, int y, Direction heading, String expected) {
        assertEquals(expected, run(x, y, heading, "M"));
    }

    @Test
    void roverStopsAtEdgeAndContinuesAfterwards() {
        // Bloqué au nord après 5 pas, puis tourne et repart vers l'est
        assertEquals("2 5 E", run(0, 0, Direction.N, "MMMMMMMMRMM"));
    }

    @Test
    void roverOnSingleCellPlateauNeverMoves() {
        Rover rover = new Rover(0, 0, Direction.N);
        rover.execute("MRMRMRMLM", new Plateau(0, 0));
        assertEquals("0 0 S", rover.toString());
    }

    @ParameterizedTest
    @CsvSource({"X", "l", "m", "MMX", "' '"})
    void rejectsUnknownInstruction(String instructions) {
        Rover rover = new Rover(1, 1, Direction.N);
        assertThrows(IllegalArgumentException.class, () -> rover.execute(instructions, plateau));
    }

    @Test
    void instructionsBeforeAnInvalidOneAreStillApplied() {
        Rover rover = new Rover(1, 1, Direction.N);
        assertThrows(IllegalArgumentException.class, () -> rover.execute("MMXM", plateau));
        assertEquals("1 3 N", rover.toString());
    }
}
