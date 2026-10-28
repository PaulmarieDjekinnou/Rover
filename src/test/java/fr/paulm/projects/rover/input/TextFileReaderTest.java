package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.TestResources;
import fr.paulm.projects.rover.domain.RoverInstructions;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFileReaderTest {

    @TempDir
    Path tempDir;

    // Écrit le contenu dans un fichier temporaire, puis le lit comme le ferait le programme.
    private SimulationInput parse(String content) {
        Path file = tempDir.resolve("input.txt");
        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new TextFileReader(file).read();
    }

    private String errorFor(String content) {
        return assertThrows(InvalidInputException.class, () -> parse(content)).getMessage();
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
            "outside-plateau                   | Le rover démarre hors du plateau : « 6 2 »",
            "unknown-instruction               | Instruction inconnue : « X ». Attendu : L, R ou M",
    })
    void invalidFile(String name, String message) throws Exception {
        TextFileReader reader = new TextFileReader(TestResources.path("simulations/invalid/" + name + ".txt"));
        assertEquals(message, assertThrows(InvalidInputException.class, reader::read).getMessage());
    }

    @Test
    void missingFileIsReportedAsUnreadable() {
        Path missing = tempDir.resolve("absent.txt");
        assertThrows(UncheckedIOException.class, () -> new TextFileReader(missing).read());
    }

    // Cas d'erreur sur une seule valeur

    @ParameterizedTest
    @ValueSource(strings = {"", "\n\n", "   \n  "})
    void emptyInputIsRejected(String content) {
        assertEquals("Fichier vide : la première ligne doit donner la taille du plateau, par exemple « 5 5 »", errorFor(content));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "5 5 5", "a 5", "5 b", "5.5 5"})
    void invalidPlateauLineIsRejected(String sizeLine) {
        assertEquals("Taille de plateau invalide : « " + sizeLine + " ». Attendu : deux entiers, par exemple « 5 5 »",
                errorFor(sizeLine));
    }

    @Test
    void negativePlateauSizeIsRejected() {
        assertEquals("Taille de plateau invalide : « -1 5 ». Les dimensions ne peuvent pas être négatives", errorFor("-1 5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2", "1 2 N E", "N", "x 2 N", "1 y N", "1.5 2 N"})
    void invalidPositionLineIsRejected(String positionLine) {
        assertEquals("Position invalide : « " + positionLine + " ». Attendu : x y direction, par exemple « 1 2 N »",
                errorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1 0", "0 -1", "6 0", "0 6"})
    void roverStartingOutsidePlateauIsRejected(String coordinates) {
        assertEquals("Le rover démarre hors du plateau : « " + coordinates + " »", errorFor("5 5\n" + coordinates + " N\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "n", "NORTH"})
    void invalidDirectionIsRejected(String direction) {
        assertEquals("Direction invalide : « " + direction + " ». Attendu : N, E, S ou W",
                errorFor("5 5\n1 2 " + direction + "\nM\n"));
    }

    @ParameterizedTest
    @CsvSource({"MXM, X", "l, l", "M M, ' '"})
    void unknownInstructionIsRejected(String instructions, char unknown) {
        assertEquals("Instruction inconnue : « " + unknown + " ». Attendu : L, R ou M",
                errorFor("5 5\n1 2 N\n" + instructions + "\n"));
    }

    @Test
    void errorOnLastRoverRejectsWholeFile() {
        // Le premier rover est valide, mais rien n'est renvoyé : le fichier est refusé en entier.
        assertEquals("Direction invalide : « X ». Attendu : N, E, S ou W", errorFor("5 5\n1 2 N\nM\n3 3 X\nM\n"));
    }
}
