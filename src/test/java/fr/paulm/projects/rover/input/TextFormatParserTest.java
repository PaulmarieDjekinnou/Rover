package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.RoverMission;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFormatParserTest {

    private final TextFormatParser parser = new TextFormatParser();

    private SimulationInput parse(String content) {
        return parser.parse(content);
    }

    private String errorFor(String content) {
        return assertThrows(InvalidInputException.class, () -> parse(content)).getMessage();
    }

    // Nominal cases

    @Test
    void parsesPlateauAndRoversInOrder() {
        SimulationInput input = parse("""
                5 3
                1 2 N
                LMLMLMLMM
                3 3 E
                MMRMMRMRRM
                """);

        assertTrue(input.plateau().contains(5, 3));
        assertFalse(input.plateau().contains(5, 4));

        List<RoverMission> missions = input.missions();
        assertEquals(2, missions.size());
        assertEquals(new Rover(1, 2, Direction.N), missions.get(0).rover());
        assertEquals(TestInstructions.of("LMLMLMLMM"), missions.get(0).instructions());
        assertEquals(new Rover(3, 3, Direction.E), missions.get(1).rover());
        assertEquals(TestInstructions.of("MMRMMRMRRM"), missions.get(1).instructions());
    }

    @Test
    void blankLinesAndExtraSpacesAreIgnored() {
        SimulationInput input = parse("\n  5   5\n\n 1  2   N \n   LM \n\n");
        assertEquals(new Rover(1, 2, Direction.N), input.missions().get(0).rover());
        assertEquals(TestInstructions.of("LM"), input.missions().get(0).instructions());
    }

    @Test
    void plateauWithoutRoversHasNoRovers() {
        assertEquals(List.of(), parse("5 5\n").missions());
    }

    // Error cases

    @ParameterizedTest
    @ValueSource(strings = {"", "\n\n", "   \n  "})
    void emptyInputIsRejected(String content) {
        assertEquals("Empty file: the first line must give the plateau size, for example \"5 5\"", errorFor(content));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "5 5 5", "a 5", "5 b", "5.5 5"})
    void invalidPlateauLineIsRejected(String sizeLine) {
        assertEquals("Invalid plateau size: \"" + sizeLine + "\". Expected: two integers, for example \"5 5\"",
                errorFor(sizeLine));
    }

    @Test
    void negativePlateauSizeIsRejected() {
        assertEquals("Invalid plateau size: \"-1 5\". Dimensions cannot be negative", errorFor("-1 5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2", "1 2 N E", "N", "x 2 N", "1 y N", "1.5 2 N"})
    void invalidPositionLineIsRejected(String positionLine) {
        assertEquals("Invalid position: \"" + positionLine + "\". Expected: x y direction, for example \"1 2 N\"",
                errorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1 0", "0 -1", "6 0", "0 6"})
    void roverStartingOutsidePlateauIsRejected(String coordinates) {
        assertEquals("Rover starts outside the plateau: \"" + coordinates + "\"", errorFor("5 5\n" + coordinates + " N\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "n", "NORTH"})
    void invalidDirectionIsRejected(String direction) {
        assertEquals("Invalid direction: \"" + direction + "\". Expected: N, E, S or W",
                errorFor("5 5\n1 2 " + direction + "\nM\n"));
    }

    @ParameterizedTest
    @CsvSource({"MXM, X", "l, l", "M M, ' '"})
    void unknownInstructionIsRejected(String instructions, char unknown) {
        assertEquals("Unknown instruction: \"" + unknown + "\". Expected: L, R or M",
                errorFor("5 5\n1 2 N\n" + instructions + "\n"));
    }

    @Test
    void errorOnLastRoverRejectsWholeFile() {
        // The first rover is valid, but nothing is returned: the whole file is rejected.
        assertEquals("Invalid direction: \"X\". Expected: N, E, S or W", errorFor("5 5\n1 2 N\nM\n3 3 X\nM\n"));
    }
}
