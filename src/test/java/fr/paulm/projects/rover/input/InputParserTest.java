package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.TestResources;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.RoverInstructions;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.exception.InvalidInputException;
import fr.paulm.projects.rover.domain.exception.InvalidSimulationException;
import fr.paulm.projects.rover.domain.exception.RoverOutsidePlateauException;
import fr.paulm.projects.rover.input.exception.InputFormatException;
import fr.paulm.projects.rover.input.exception.InvalidDirectionException;
import fr.paulm.projects.rover.input.exception.UnknownInstructionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputParserTest {

    private final InputParser parser = new InputParser();

    private SimulationInput parse(String content) {
        return parser.parse(content.lines().toList());
    }

    private <E extends InvalidInputException> E errorFor(Class<E> type, String content) {
        return assertThrows(type, () -> parse(content));
    }

    // Message d'une erreur de format.
    private String formatErrorFor(String content) {
        return errorFor(InputFormatException.class, content).getMessage();
    }

    // Cas nominaux

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

        List<RoverInstructions> rovers = input.rovers();
        assertEquals(2, rovers.size());
        assertEquals("1 2 N", rovers.get(0).rover().toString());
        assertEquals(TestInstructions.of("LMLMLMLMM"), rovers.get(0).instructions());
        assertEquals("3 3 E", rovers.get(1).rover().toString());
        assertEquals(TestInstructions.of("MMRMMRMRRM"), rovers.get(1).instructions());
    }

    @Test
    void blankLinesAndExtraSpacesAreIgnored() {
        SimulationInput input = parse("\n  5   5\n\n 1  2   N \n   LM \n\n");
        assertEquals("1 2 N", input.rovers().get(0).rover().toString());
        assertEquals(TestInstructions.of("LM"), input.rovers().get(0).instructions());
    }

    @Test
    void plateauWithoutRoversHasNoRovers() {
        assertEquals(List.of(), parse("5 5\n").rovers());
    }

    // Cas d'erreur : fichiers complets de simulations/invalid, avec le message attendu

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "missing-instructions              | Chaque rover doit avoir une ligne de position et une ligne d'instructions",
            "missing-instructions-second-rover | Chaque rover doit avoir une ligne de position et une ligne d'instructions",
            "outside-plateau                   | Le rover démarre hors du plateau : 6 2",
            "unknown-instruction               | Instruction inconnue : X",
    })
    void invalidFile(String name, String message) throws Exception {
        List<String> lines = TestResources.read("simulations/invalid/" + name + ".txt");
        assertEquals(message, assertThrows(InvalidInputException.class, () -> parser.parse(lines)).getMessage());
    }

    // Cas d'erreur sur une seule valeur

    @ParameterizedTest
    @ValueSource(strings = {"", "\n\n", "   \n  "})
    void emptyInputIsRejected(String content) {
        assertEquals("Fichier vide", formatErrorFor(content));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "5 5 5"})
    void plateauLineWithWrongNumberOfValuesIsRejected(String sizeLine) {
        assertEquals("Taille de plateau invalide : " + sizeLine, formatErrorFor(sizeLine));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a 5", "5 b", "5.5 5"})
    void nonNumericPlateauSizeIsRejected(String sizeLine) {
        assertEquals("Taille de plateau invalide : " + sizeLine, formatErrorFor(sizeLine));
    }

    @Test
    void negativePlateauSizeIsASimulationRuleError() {
        // Le format est correct : c'est le constructeur de Plateau qui refuse la valeur.
        InvalidSimulationException e = errorFor(InvalidSimulationException.class, "-1 5");
        assertTrue(e.getMessage().startsWith("Taille de plateau invalide"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2", "1 2 N E", "N"})
    void positionLineWithWrongNumberOfValuesIsRejected(String positionLine) {
        assertEquals("Position invalide : " + positionLine, formatErrorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x 2 N", "1 y N", "1.5 2 N"})
    void nonNumericPositionIsRejected(String positionLine) {
        assertEquals("Position invalide : " + positionLine, formatErrorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1", "6, 0", "0, 6"})
    void roverStartingOutsidePlateauIsRejected(int x, int y) {
        RoverOutsidePlateauException e = errorFor(RoverOutsidePlateauException.class, "5 5\n" + x + " " + y + " N\nM\n");
        assertEquals(x, e.x());
        assertEquals(y, e.y());
        assertEquals("Le rover démarre hors du plateau : " + x + " " + y, e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "n", "NORTH"})
    void invalidDirectionIsRejected(String direction) {
        InvalidDirectionException e = errorFor(InvalidDirectionException.class, "5 5\n1 2 " + direction + "\nM\n");
        assertEquals(direction, e.value());
        assertEquals("Direction invalide : " + direction, e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"MXM, X", "l, l", "M M, ' '"})
    void unknownInstructionIsRejected(String instructions, char unknown) {
        UnknownInstructionException e = errorFor(UnknownInstructionException.class, "5 5\n1 2 N\n" + instructions + "\n");
        assertEquals(unknown, e.instruction());
        assertEquals("Instruction inconnue : " + unknown, e.getMessage());
    }

    @Test
    void errorOnLastRoverRejectsWholeFile() {
        // Le premier rover est valide, mais rien n'est renvoyé : le fichier est refusé en entier.
        assertEquals("X", errorFor(InvalidDirectionException.class, "5 5\n1 2 N\nM\n3 3 X\nM\n").value());
    }
}
