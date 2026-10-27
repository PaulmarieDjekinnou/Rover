package fr.paulm.projects;

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

    private String errorFor(String content) {
        return assertThrows(IllegalArgumentException.class, () -> parse(content)).getMessage();
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
        assertEquals("LMLMLMLMM", rovers.get(0).instructions());
        assertEquals("3 3 E", rovers.get(1).rover().toString());
        assertEquals("MMRMMRMRRM", rovers.get(1).instructions());
    }

    @Test
    void blankLinesAndExtraSpacesAreIgnored() {
        SimulationInput input = parse("\n  5   5\n\n 1  2   N \n   LM \n\n");
        assertEquals("1 2 N", input.rovers().get(0).rover().toString());
        assertEquals("LM", input.rovers().get(0).instructions());
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
        assertEquals(message, assertThrows(IllegalArgumentException.class, () -> parser.parse(lines)).getMessage());
    }

    // Cas d'erreur sur une seule valeur

    @ParameterizedTest
    @ValueSource(strings = {"", "\n\n", "   \n  "})
    void emptyInputIsRejected(String content) {
        assertEquals("Fichier vide", errorFor(content));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "5 5 5"})
    void plateauLineWithWrongNumberOfValuesIsRejected(String sizeLine) {
        assertEquals("Taille de plateau invalide : " + sizeLine, errorFor(sizeLine));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a 5", "5 b", "5.5 5"})
    void nonNumericPlateauSizeIsRejected(String sizeLine) {
        assertEquals("Taille de plateau invalide : " + sizeLine, errorFor(sizeLine));
    }

    @Test
    void negativePlateauSizeIsRejected() {
        assertTrue(errorFor("-1 5").startsWith("Taille de plateau invalide"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2", "1 2 N E", "N"})
    void positionLineWithWrongNumberOfValuesIsRejected(String positionLine) {
        assertEquals("Position invalide : " + positionLine, errorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x 2 N", "1 y N", "1.5 2 N"})
    void nonNumericPositionIsRejected(String positionLine) {
        assertEquals("Position invalide : " + positionLine, errorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1 0", "0 -1", "6 0", "0 6"})
    void roverStartingOutsidePlateauIsRejected(String coordinates) {
        assertEquals("Le rover démarre hors du plateau : " + coordinates,
                errorFor("5 5\n" + coordinates + " N\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "n", "NORTH"})
    void invalidDirectionIsRejected(String direction) {
        assertEquals("Direction invalide : " + direction, errorFor("5 5\n1 2 " + direction + "\nM\n"));
    }

    @ParameterizedTest
    @CsvSource({"MXM, X", "l, l", "M M, ' '"})
    void unknownInstructionIsRejected(String instructions, char unknown) {
        assertEquals("Instruction inconnue : " + unknown, errorFor("5 5\n1 2 N\n" + instructions + "\n"));
    }

    @Test
    void errorOnLastRoverRejectsWholeFile() {
        // Le premier rover est valide, mais rien n'est renvoyé : le fichier est refusé en entier.
        assertEquals("Direction invalide : X", errorFor("5 5\n1 2 N\nM\n3 3 X\nM\n"));
    }
}
