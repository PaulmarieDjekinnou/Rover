package fr.paulm.projects.rover;

import fr.paulm.projects.rover.input.SimulationReader;
import fr.paulm.projects.rover.input.TextFileReader;
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
// The history and the results are written to a buffer given to the ConsoleReport, the errors are read from Main's logs
// and the warnings from ConsoleReport's logs.
class MainTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ConsoleReport report = new ConsoleReport(new PrintStream(output, true));
    private LogCapture logs;
    private LogCapture warnings;

    @BeforeEach
    void capture() {
        logs = LogCapture.start(Main.class);
        warnings = LogCapture.start(ConsoleReport.class);
    }

    @AfterEach
    void stopCapture() {
        logs.stop();
        warnings.stop();
    }

    // The program, writing to the buffer instead of the standard output.
    private Main main() {
        return new Main(TextFileReader::new, report);
    }

    private List<String> outputLines() {
        return output.toString().lines().toList();
    }

    // Each simulations/valid/<name>.txt file is run through the program and compared to <name>.expected.txt
    @ParameterizedTest
    @ValueSource(strings = {"sample", "blank-lines", "collision", "wrap-around", "no-rovers", "mini-planet", "mars"})
    void validFile(String name) throws Exception {
        Path input = TestResources.path("simulations/valid/" + name + ".txt");
        assertEquals(0, main().run(new String[]{input.toString()}));
        List<String> expected = TestResources.read("simulations/valid/" + name + ".expected.txt");
        assertEquals(expected, outputLines());
        assertEquals(List.of(), logs.messages());
    }

    @Test
    void blockedMovesAreReportedPerRover() throws Exception {
        Path input = TestResources.path("simulations/valid/collision.txt");
        main().run(new String[]{input.toString()});
        assertEquals(List.of(
                "Rover 1: 1 move(s) blocked by another rover.",
                "Rover 2: 3 move(s) blocked by another rover."),
                warnings.messages());
    }

    @Test
    void movesBlockedByTheTerrainAreReportedByRoverName() throws Exception {
        Path input = TestResources.path("simulations/valid/mini-planet.txt");
        main().run(new String[]{input.toString()});
        assertEquals(List.of("Climber: 1 move(s) blocked by the terrain."), warnings.messages());
    }

    @Test
    void historyOnAPlanetNamesTheBlockedRovers() throws Exception {
        Path input = TestResources.path("simulations/valid/mini-planet.txt");
        assertEquals(0, main().run(new String[]{"--simultaneous", "--history", input.toString()}));
        // Polar crosses the north pole in turn 2: it arrives on the other side of the planet, facing south
        assertEquals(List.of(
                "Turn 1: 1 3 N | 4 1 S",
                "Turn 2: 5 3 S | 4 1 W",
                "Turn 3: 5 2 S | 4 1 W (blocked: Climber)"),
                outputLines().subList(3, 6));
    }

    @Test
    void invalidLandingSiteIsReportedWithItsLine() throws Exception {
        Path input = TestResources.path("simulations/invalid/landing-site-too-high.txt");
        assertEquals(1, main().run(new String[]{input.toString()}));
        assertEquals(List.of("Invalid input: Line 4: Landing site 18.65N 226.20E is too high (20009 m). Expected: at most -1000 m"),
                logs.messages());
        assertEquals(List.of(), outputLines());
    }

    @Test
    void noWarningWithoutCollision() throws Exception {
        Path input = TestResources.path("simulations/valid/sample.txt");
        main().run(new String[]{input.toString()});
        assertEquals(List.of(), warnings.messages());
    }

    @Test
    void stepByStepOptionMovesTheRoversTurnByTurn() throws Exception {
        Path input = TestResources.path("simulations/valid/collision.txt");
        assertEquals(0, main().run(new String[]{"--step-by-step", input.toString()}));
        // Sequential mode gives "0 2 N" and "0 3 S" (see collision.expected.txt)
        assertEquals(List.of("0 1 N", "0 2 S"), outputLines());
        assertEquals(List.of(
                "Rover 1: 2 move(s) blocked by another rover.",
                "Rover 2: 2 move(s) blocked by another rover."),
                warnings.messages());
    }

    @Test
    void simultaneousOptionMovesTheRoversAtTheSameTime() throws Exception {
        Path input = TestResources.path("simulations/valid/collision.txt");
        assertEquals(0, main().run(new String[]{"--simultaneous", input.toString()}));
        // Turn 1: both rovers move; turns 2 and 3: they would swap their cells, so both are blocked
        assertEquals(List.of("0 1 N", "0 2 S"), outputLines());
        assertEquals(List.of(
                "Rover 1: 2 move(s) blocked by another rover.",
                "Rover 2: 2 move(s) blocked by another rover."),
                warnings.messages());
    }

    @Test
    void historyOptionPrintsEachTurnBeforeTheFinalPositions() throws Exception {
        Path input = TestResources.path("simulations/valid/collision.txt");
        assertEquals(0, main().run(new String[]{"--history", input.toString()}));
        // Sequential mode: one turn per instruction, the first rover's three, then the second rover's three
        assertEquals(List.of(
                "Turn 1: 0 1 N | 0 3 S",
                "Turn 2: 0 2 N | 0 3 S",
                "Turn 3: 0 2 N | 0 3 S (blocked: 1)",
                "Turn 4: 0 2 N | 0 3 S (blocked: 2)",
                "Turn 5: 0 2 N | 0 3 S (blocked: 2)",
                "Turn 6: 0 2 N | 0 3 S (blocked: 2)",
                "0 2 N",
                "0 3 S"), outputLines());
    }

    @Test
    void historyOptionWorksWithAnotherMode() throws Exception {
        Path input = TestResources.path("simulations/valid/collision.txt");
        assertEquals(0, main().run(new String[]{"--simultaneous", "--history", input.toString()}));
        assertEquals(List.of(
                "Turn 1: 0 1 N | 0 2 S",
                "Turn 2: 0 1 N | 0 2 S (blocked: 1, 2)",
                "Turn 3: 0 1 N | 0 2 S (blocked: 1, 2)",
                "0 1 N",
                "0 2 S"), outputLines());
    }

    @Test
    void unexpectedErrorIsReported() {
        SimulationReader failingReader = () -> {
            throw new IllegalStateException("boom");
        };
        assertEquals(1, new Main(path -> failingReader, report).run(new String[]{"input.txt"}));
        assertEquals(List.of("Unexpected error: java.lang.IllegalStateException: boom"), logs.messages());
        assertEquals(List.of(), outputLines());
    }

    @Test
    void invalidArgumentsPrintUsage() {
        // Every invalid combination is tested in CommandLineTest
        assertEquals(1, main().run(new String[]{}));
        assertEquals(List.of(CommandLine.usage()), logs.messages());
    }

    @Test
    void missingFileIsReported() {
        Path missing = tempDir.resolve("absent.txt");
        assertEquals(1, main().run(new String[]{missing.toString()}));
        assertEquals(List.of("Cannot read file \"" + missing + "\": file not found"), logs.messages());
    }

    @Test
    void invalidPathIsReported() {
        // A nul character can never be part of a path, on any operating system.
        assertEquals(1, main().run(new String[]{"a\0b"}));
        assertEquals(List.of("Invalid file path: \"a\0b\". Nul character not allowed"), logs.messages());
    }

    @Test
    void invalidContentIsReported() throws Exception {
        Path input = TestResources.path("simulations/invalid/unknown-instruction.txt");
        assertEquals(1, main().run(new String[]{input.toString()}));
        assertEquals(List.of("Invalid input: Line 5: Unknown instruction: \"X\". Expected: L, R, M or B"),
                logs.messages());
        assertEquals(List.of(), outputLines());
    }
}
