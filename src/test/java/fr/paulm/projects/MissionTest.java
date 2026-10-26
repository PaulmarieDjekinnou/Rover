package fr.paulm.projects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private List<String> printedLines() {
        return out.toString().lines().toList();
    }

    // Exécute la mission et renvoie ce que display() a affiché, ligne par ligne.
    private List<String> process(List<String> lines) {
        Mission mission = new Mission(lines);
        mission.run();
        mission.display();
        return printedLines();
    }

    private List<String> process(String content) {
        return process(content.lines().toList());
    }

    private String errorFor(String content) {
        return assertThrows(IllegalArgumentException.class, () -> process(content)).getMessage();
    }

    // Lit un fichier de src/test/resources depuis le classpath.
    static List<String> readResource(String name) throws IOException, URISyntaxException {
        URL url = MissionTest.class.getResource("/" + name);
        assertNotNull(url, "Fichier de test introuvable : " + name);
        return Files.readAllLines(Path.of(url.toURI()));
    }

    // Cas nominaux : chaque fichier missions/valid/<nom>.txt est comparé à <nom>.expected.txt

    @ParameterizedTest
    @ValueSource(strings = {"sample", "blank-lines", "same-cell", "stuck-at-edge", "no-rovers"})
    void validMission(String name) throws Exception {
        List<String> input = readResource("missions/valid/" + name + ".txt");
        List<String> expected = readResource("missions/valid/" + name + ".expected.txt");
        assertEquals(expected, process(input));
    }

    @Test
    void displayBeforeRunPrintsNothing() {
        Mission mission = new Mission(List.of("5 5", "1 2 N", "M"));
        mission.display();
        assertEquals("", out.toString());
    }

    @Test
    void runningTwiceDoesNotDuplicateRovers() {
        Mission mission = new Mission(List.of("5 5", "1 2 N", "M"));
        mission.run();
        mission.run();
        mission.display();
        assertEquals(List.of("1 3 N"), printedLines());
    }

    // Cas d'erreur : fichiers complets de missions/invalid, avec le message attendu

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "missing-instructions              | Chaque rover doit avoir une ligne de position et une ligne d'instructions",
            "missing-instructions-second-rover | Chaque rover doit avoir une ligne de position et une ligne d'instructions",
            "outside-plateau                   | Le rover démarre hors du plateau : 6 2",
            "unknown-instruction               | Instruction inconnue : X",
    })
    void invalidMission(String name, String message) throws Exception {
        List<String> input = readResource("missions/invalid/" + name + ".txt");
        assertEquals(message, assertThrows(IllegalArgumentException.class, () -> process(input)).getMessage());
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
        assertTrue(errorFor(sizeLine).startsWith("Taille de plateau invalide"));
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
        String message = errorFor("5 5\n" + positionLine + "\nM\n");
        assertTrue(message.startsWith("Position invalide"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x 2 N", "1 y N", "1.5 2 N"})
    void nonNumericPositionIsRejected(String positionLine) {
        assertEquals("Position invalide : " + positionLine, errorFor("5 5\n" + positionLine + "\nM\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "n", "NORTH"})
    void invalidDirectionIsRejected(String direction) {
        assertEquals("Direction invalide : " + direction, errorFor("5 5\n1 2 " + direction + "\nM\n"));
    }
}
