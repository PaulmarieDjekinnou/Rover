package fr.paulm.projects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private List<String> process(String content) {
        Mission mission = new Mission(content.lines().toList());
        mission.run();
        mission.display();
        return printedLines();
    }

    private String errorFor(String content) {
        return assertThrows(IllegalArgumentException.class, () -> process(content)).getMessage();
    }

    // Cas nominaux

    @Test
    void sampleInput() {
        List<String> output = process("""
                5 5
                1 2 N
                LMLMLMLMM
                3 3 E
                MMRMMRMRRM
                """);
        assertEquals(List.of("1 3 N", "5 1 E"), output);
    }

    @Test
    void blankLinesAndExtraSpacesAreIgnored() {
        List<String> output = process("""

                  5   5

                1  2   N
                   LMLMLMLMM

                """);
        assertEquals(List.of("1 3 N"), output);
    }

    @Test
    void plateauWithoutRoversReturnsNothing() {
        assertEquals(List.of(), process("5 5\n"));
    }

    @Test
    void roversAreProcessedIndependently() {
        // Deux rovers sur la même case : pas de détection de collision
        List<String> output = process("""
                3 3
                0 0 N
                MMM
                0 0 E
                MMM
                """);
        assertEquals(List.of("0 3 N", "3 0 E"), output);
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

    // Cas d'erreur

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

    @Test
    void missingInstructionLineIsRejected() {
        String message = errorFor("""
                5 5
                1 2 N
                """);
        assertEquals("Chaque rover doit avoir une ligne de position et une ligne d'instructions", message);
    }

    @Test
    void missingInstructionLineForSecondRoverIsRejected() {
        String message = errorFor("""
                5 5
                1 2 N
                LMLMLMLMM
                3 3 E
                """);
        assertEquals("Chaque rover doit avoir une ligne de position et une ligne d'instructions", message);
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

    @Test
    void roverStartingOutsidePlateauIsRejected() {
        assertTrue(errorFor("5 5\n6 2 N\nM\n").startsWith("Le rover démarre hors du plateau"));
    }

    @Test
    void unknownInstructionIsRejected() {
        assertEquals("Instruction inconnue : X", errorFor("5 5\n1 2 N\nMXM\n"));
    }
}
