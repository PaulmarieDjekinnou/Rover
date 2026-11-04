package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.RoverMission;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
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

        assertTrue(input.plateau().contains(new Position(5, 3)));
        assertFalse(input.plateau().contains(new Position(5, 4)));

        List<RoverMission> missions = input.missions();
        assertEquals(2, missions.size());
        assertEquals(new Rover(new Position(1, 2), Direction.N), missions.get(0).rover());
        assertEquals(TestInstructions.of("LMLMLMLMM"), missions.get(0).instructions());
        assertEquals(new Rover(new Position(3, 3), Direction.E), missions.get(1).rover());
        assertEquals(TestInstructions.of("MMRMMRMRRM"), missions.get(1).instructions());
    }

    @Test
    void blankLinesAndExtraSpacesAreIgnored() {
        SimulationInput input = parse("\n  5   5\n\n 1  2   N \n   LM \n\n");
        assertEquals(new Rover(new Position(1, 2), Direction.N), input.missions().get(0).rover());
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
        assertEquals("Line 1: Invalid plateau size: \"" + sizeLine + "\". Expected: two integers, for example \"5 5\"",
                errorFor(sizeLine));
    }

    @Test
    void negativePlateauSizeIsRejected() {
        assertEquals("Line 1: Invalid plateau size: \"-1 5\". Dimensions cannot be negative", errorFor("-1 5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2", "1 2 N E", "N", "x 2 N", "1 y N", "1.5 2 N"})
    void invalidPositionLineIsRejected(String positionLine) {
        assertEquals("Line 2: Invalid position: \"" + positionLine + "\". Expected: x y direction, for example \"1 2 N\"",
                errorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @Test
    void roverStartingOutsidePlateauIsRejected() {
        // The rule belongs to SimulationInput (tested there on every side); this only checks it applies after parsing.
        assertEquals("Rover starts outside the plateau: \"6 2\"", errorFor("5 5\n6 2 N\nM\n"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "5 5\\n1 2 N | 2",
            "5 5\\n1 2 N\\nLMLMLMLMM\\n3 3 E | 4",
            // The error points to the rover without instructions, not to a later line
            "5 5\\n1 2 N\\n\\n3 3 E\\nMM | 2",
            "5 5\\n1 2 N\\n\\n3 3 E\\n | 2",
            "5 5\\n1 2 N\\n-1 0 E\\nMM | 2"})
    void missingInstructionLineIsRejected(String content, int lineNumber) {
        assertEquals("Line " + lineNumber + ": Each rover needs a position line and an instruction line",
                errorFor(content.replace("\\n", "\n")));
    }

    @Test
    void lineNumbersCountBlankLines() {
        // The direction is on the 4th line of the content, after two blank lines
        assertEquals("Line 4: Invalid direction: \"X\". Expected: N, E, S or W", errorFor("5 5\n\n\n1 2 X\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "n", "NORTH"})
    void invalidDirectionIsRejected(String direction) {
        assertEquals("Line 2: Invalid direction: \"" + direction + "\". Expected: N, E, S or W",
                errorFor("5 5\n1 2 " + direction + "\nM\n"));
    }

    @ParameterizedTest
    @CsvSource({"MXM, X", "l, l", "M M, ' '"})
    void unknownInstructionIsRejected(String instructions, char unknown) {
        assertEquals("Line 3: Unknown instruction: \"" + unknown + "\". Expected: L, R, M or B",
                errorFor("5 5\n1 2 N\n" + instructions + "\n"));
    }

    @Test
    void errorOnLastRoverRejectsWholeFile() {
        // The first rover is valid, but nothing is returned: the whole file is rejected.
        assertEquals("Line 4: Invalid direction: \"X\". Expected: N, E, S or W", errorFor("5 5\n1 2 N\nM\n3 3 X\nM\n"));
    }
}
