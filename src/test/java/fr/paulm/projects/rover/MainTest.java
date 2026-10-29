package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Simulation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// End-to-end tests of the program: Main.run returns the exit code instead of calling System.exit.
class MainTest {

    @TempDir
    Path tempDir;

    private LogCapture logs;
    private LogCapture errors;

    @BeforeEach
    void capture() {
        logs = LogCapture.start(Simulation.class);
        errors = LogCapture.start(Main.class);
    }

    @AfterEach
    void stopCapture() {
        logs.stop();
        errors.stop();
    }

    // Each simulations/valid/<name>.txt file is run through the program and compared to <name>.expected.txt
    @ParameterizedTest
    @ValueSource(strings = {"sample", "blank-lines", "same-cell", "stuck-at-edge", "no-rovers"})
    void validFile(String name) throws Exception {
        Path input = TestResources.path("simulations/valid/" + name + ".txt");
        assertEquals(ExitCode.SUCCESS, new Main().run(new String[]{input.toString()}));
        List<String> expected = TestResources.read("simulations/valid/" + name + ".expected.txt");
        assertEquals(expected, logs.messages());
        assertEquals(List.of(), errors.messages());
    }

    @Test
    void missingArgumentPrintsUsage() {
        assertEquals(ExitCode.USAGE_ERROR, new Main().run(new String[]{}));
        assertEquals(List.of("Usage: java -jar rover.jar input.txt"), errors.messages());
    }

    @Test
    void tooManyArgumentsPrintsUsage() {
        assertEquals(ExitCode.USAGE_ERROR, new Main().run(new String[]{"a.txt", "b.txt"}));
        assertEquals(List.of("Usage: java -jar rover.jar input.txt"), errors.messages());
    }

    @Test
    void missingFileIsReported() {
        Path missing = tempDir.resolve("absent.txt");
        assertEquals(ExitCode.UNREADABLE_FILE, new Main().run(new String[]{missing.toString()}));
        assertEquals(List.of("Cannot read file: " + missing), errors.messages());
    }

    @Test
    void invalidPathIsReported() {
        // A nul character can never be part of a path, on any operating system.
        assertEquals(ExitCode.UNREADABLE_FILE, new Main().run(new String[]{"a\0b"}));
        assertEquals(List.of("Invalid file path: \"a\0b\". Nul character not allowed"), errors.messages());
    }

    @Test
    void invalidContentIsReported() throws Exception {
        Path input = TestResources.path("simulations/invalid/unknown-instruction.txt");
        assertEquals(ExitCode.INVALID_INPUT, new Main().run(new String[]{input.toString()}));
        assertEquals(List.of("Invalid input: Unknown instruction: \"X\". Expected: L, R or M"), errors.messages());
        assertEquals(List.of(), logs.messages());
    }
}
