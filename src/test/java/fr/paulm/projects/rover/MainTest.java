package fr.paulm.projects.rover;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Test de bout en bout du cas nominal : les cas d'erreur appellent System.exit.
class MainTest {

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

    @Test
    void printsFinalPositionsForSampleFile() throws Exception {
        Path input = TestResources.path("simulations/valid/sample.txt");
        Main.main(new String[]{input.toString()});
        List<String> expected = TestResources.read("simulations/valid/sample.expected.txt");
        assertEquals(expected, out.toString().lines().toList());
    }
}
