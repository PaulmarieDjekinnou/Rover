package fr.paulm.projects.rover;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// End-to-end tests of the program: Main.run returns the exit code instead of calling System.exit.
// The results are read from the standard output, the warnings and errors from Main's logs.
class MainTest {

    private static final String USAGE = "Usage: java -jar rover.jar [--step-by-step] input.txt";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private LogCapture logs;

    @BeforeEach
    void capture() {
        originalOut = System.out;
        System.setOut(new PrintStream(output, true));
        logs = LogCapture.start(Main.class);
    }

    @AfterEach
    void stopCapture() {
        System.setOut(originalOut);
        logs.stop();
    }

    private List<String> outputLines() {
        return output.toString().lines().toList();
    }

    // Each simulations/valid/<name>.txt file is run through the program and compared to <name>.expected.txt
    @ParameterizedTest
    @ValueSource(strings = {"sample", "blank-lines", "collision", "wrap-around", "no-rovers"})
    void validFile(String name) throws Exception {
        Path input = TestResources.path("simulations/valid/" + name + ".txt");
        assertEquals(0, new Main().run(new String[]{input.toString()}));
        List<String> expected = TestResources.read("simulations/valid/" + name + ".expected.txt");
        assertEquals(expected, outputLines());
        assertEquals(List.of(), logs.messages(Level.ERROR));
    }

    @Test
    void blockedMovesAreReportedPerRover() throws Exception {
        Path input = TestResources.path("simulations/valid/collision.txt");
        new Main().run(new String[]{input.toString()});
        assertEquals(List.of(
                "Rover 1: 1 move(s) ignored because the cell is occupied by another rover.",
                "Rover 2: 3 move(s) ignored because the cell is occupied by another rover."),
                logs.messages(Level.WARN));
    }

    @Test
    void noWarningWithoutCollision() throws Exception {
        Path input = TestResources.path("simulations/valid/sample.txt");
        new Main().run(new String[]{input.toString()});
        assertEquals(List.of(), logs.messages());
    }

    @Test
    void stepByStepOptionMovesTheRoversTurnByTurn() throws Exception {
        Path input = TestResources.path("simulations/valid/collision.txt");
        assertEquals(0, new Main().run(new String[]{"--step-by-step", input.toString()}));
        // Sequential mode gives "0 2 N" and "0 3 S" (see collision.expected.txt)
        assertEquals(List.of("0 1 N", "0 2 S"), outputLines());
        assertEquals(List.of(
                "Rover 1: 2 move(s) ignored because the cell is occupied by another rover.",
                "Rover 2: 2 move(s) ignored because the cell is occupied by another rover."),
                logs.messages(Level.WARN));
    }

    @Test
    void unknownOptionPrintsUsage() {
        assertEquals(1, new Main().run(new String[]{"--fast", "input.txt"}));
        assertEquals(List.of(USAGE), logs.messages());
    }

    @Test
    void missingArgumentPrintsUsage() {
        assertEquals(1, new Main().run(new String[]{}));
        assertEquals(List.of(USAGE), logs.messages());
    }

    @Test
    void tooManyArgumentsPrintsUsage() {
        assertEquals(1, new Main().run(new String[]{"a.txt", "b.txt"}));
        assertEquals(List.of(USAGE), logs.messages());
    }

    @Test
    void missingFileIsReported() {
        Path missing = tempDir.resolve("absent.txt");
        assertEquals(1, new Main().run(new String[]{missing.toString()}));
        assertEquals(List.of("Cannot read file \"" + missing + "\": file not found"), logs.messages());
    }

    @Test
    void invalidPathIsReported() {
        // A nul character can never be part of a path, on any operating system.
        assertEquals(1, new Main().run(new String[]{"a\0b"}));
        assertEquals(List.of("Invalid file path: \"a\0b\". Nul character not allowed"), logs.messages());
    }

    @Test
    void invalidContentIsReported() throws Exception {
        Path input = TestResources.path("simulations/invalid/unknown-instruction.txt");
        assertEquals(1, new Main().run(new String[]{input.toString()}));
        assertEquals(List.of("Invalid input: Line 5: Unknown instruction: \"X\". Expected: L, R, M or B"),
                logs.messages());
        assertEquals(List.of(), outputLines());
    }
}
