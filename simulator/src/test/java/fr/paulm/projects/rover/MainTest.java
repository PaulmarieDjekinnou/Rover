package fr.paulm.projects.rover;

import fr.paulm.projects.rover.input.SimulationReader;
import fr.paulm.projects.rover.input.TextFileReader;
import fr.paulm.projects.rover.telemetry.InMemorySink;
import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.TelemetryUnavailableException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

// End-to-end tests of the program: Main.run returns the exit code instead of calling System.exit.
// The history and the results are written to a buffer given to Main, the errors are read from Main's logs
// and the warnings from the logs of ConsoleReport (plateau) and MissionReport (planet).
class MainTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(output, true);
    private final InMemorySink sink = new InMemorySink();
    private LogCapture logs;
    private LogCapture warnings;
    private LogCapture missionWarnings;

    @BeforeEach
    void capture() {
        logs = LogCapture.start(Main.class);
        warnings = LogCapture.start(ConsoleReport.class);
        missionWarnings = LogCapture.start(MissionReport.class);
    }

    @AfterEach
    void stopCapture() {
        logs.stop();
        warnings.stop();
        missionWarnings.stop();
    }

    // The program, writing to the buffer instead of the standard output.
    private Main main() {
        return new Main(TextFileReader::new, out, server -> sink);
    }

    private List<String> outputLines() {
        return output.toString().lines().toList();
    }

    // Each simulations/valid/<name>.txt file is run through the program and compared to <name>.expected.txt
    @ParameterizedTest
    @ValueSource(strings = {"sample", "blank-lines", "collision", "wrap-around", "no-rovers", "mini-planet", "late-landing", "stranded", "mars"})
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
        assertEquals(List.of("Climber: 1 move(s) blocked by the terrain."), missionWarnings.messages());
    }

    @Test
    void historyOnAPlanetFollowsTheCommonClock() throws Exception {
        Path input = TestResources.path("simulations/valid/mini-planet.txt");
        assertEquals(0, main().run(new String[]{"--history", input.toString()}));
        // One line each time an instruction ends: both first moves end together, then Climber turns (5 minutes)
        // and is blocked by the peak at once, and Polar goes on, crossing the north pole
        // Each move is 7.9 km long: at 100 m per sol, in the drive window, about 78 sols; the turn takes 5 minutes.
        // Each rover comes with its battery charge, and (night) where the Sun is down.
        assertEquals(List.of(
                "Sol 77 22:27: Polar 67.50N 67.50E N 99% | Climber 22.50N 202.50E S 100% (night)",
                "Sol 78 13:22: Polar 67.50N 67.50E N 100% (night) | Climber 22.50S 202.50E S 99%",
                "Sol 78 13:27: Polar 67.50N 67.50E N 100% (night) | Climber 22.50S 202.50E W 99%",
                "Sol 78 13:27: Polar 67.50N 67.50E N 100% (night) | Climber 22.50S 202.50E W 99% (blocked: Climber)",
                "Sol 156 22:03: Polar 67.50N 247.50E S 100% | Climber 22.50S 202.50E W 99% (night)",
                "Sol 235 10:28: Polar 22.50N 247.50E S 99% | Climber 22.50S 202.50E W 99%"),
                outputLines().stream().filter(line -> line.startsWith("Sol ")).toList());
    }

    @Test
    void aRoverAppearsInTheHistoryOnceItHasLanded() throws Exception {
        Path input = TestResources.path("simulations/valid/late-landing.txt");
        assertEquals(0, main().run(new String[]{"--history", input.toString()}));
        // Early ends its first move on sol 71, before Late lands on sol 100
        List<String> history = outputLines().stream().filter(line -> line.startsWith("Sol ")).toList();
        assertEquals("Sol 71 22:14: Early 22.50N 112.50E E 100%", history.getFirst());
        assertTrue(history.getLast().contains("| Late "));
    }

    @Test
    void modeOptionsAreIgnoredOnAPlanet() throws Exception {
        Path input = TestResources.path("simulations/valid/mini-planet.txt");
        assertEquals(0, main().run(new String[]{"--step-by-step", input.toString()}));
        assertEquals(List.of("The --step-by-step option only applies to rovers on a plateau: a mission runs on a common clock."),
                logs.messages());
        assertEquals(TestResources.read("simulations/valid/mini-planet.expected.txt"), outputLines());
    }

    @Test
    void mapOptionDrawsTheReliefAroundEachRoverAfterTheResults() throws Exception {
        Path input = TestResources.path("simulations/valid/mini-planet.txt");
        assertEquals(0, main().run(new String[]{"--map", input.toString()}));
        // Polar crossed the north pole: its path goes through the northern row on both sides of the planet
        assertEquals(List.of(
                "",
                "Around Polar (P Polar, C Climber; lower case where they landed, ~ where they drove):",
                "Relief from 0 m (.) to 9000 m (@), north up, 8 x 4 cells:",
                "~...~...",
                "p..cP...",
                "..@C....",
                "........"), outputLines().subList(11, 18));
        // 5 lines of landings, 6 of results, then two maps of 7 lines
        assertEquals(25, outputLines().size());
    }

    @Test
    void mapOptionIsIgnoredOnAPlateau() throws Exception {
        Path input = TestResources.path("simulations/valid/sample.txt");
        assertEquals(0, main().run(new String[]{"--map", input.toString()}));
        assertEquals(List.of("1 3 N", "5 1 E"), outputLines());
        assertEquals(List.of("The --map option only applies to missions to a planet: ignored."), logs.messages());
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
        assertEquals(1, new Main(path -> failingReader, out, server -> sink).run(new String[]{"input.txt"}));
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

    @Test
    void telemetryOptionSendsTheMissionAsTheRoversWould() throws Exception {
        Path input = TestResources.path("simulations/valid/mini-planet.txt");
        assertEquals(0, main().run(new String[]{"--telemetry", "kafka:9092", input.toString()}));
        // The results are the same as without telemetry
        assertEquals(TestResources.read("simulations/valid/mini-planet.expected.txt"), outputLines());
        assertEquals(List.of(MissionStatus.Type.STARTED, MissionStatus.Type.ENDED),
                sink.statuses.stream().map(MissionStatus::type).toList());
        assertEquals(List.of(RoverEvent.Type.LANDED, RoverEvent.Type.LANDED, RoverEvent.Type.LOST, RoverEvent.Type.BLOCKED),
                sink.events.stream().map(RoverEvent::type).toList());
        // One message per instruction ended: Polar's 3 moves, Climber's move, turn and blocked move
        List<RoverTelemetry> polar = sink.telemetry.stream().filter(message -> message.rover().equals("Polar")).toList();
        assertEquals(List.of(0L, 1L, 2L), polar.stream().map(RoverTelemetry::sequence).toList());
        assertEquals(3, sink.telemetry.stream().filter(message -> message.rover().equals("Climber")).count());
        // Polar crossed the north pole: 3 moves of 7.9 km
        assertEquals(23_562, polar.getLast().distanceMeters(), 1);
        assertEquals("S", polar.getLast().direction());
        assertTrue(sink.closed);
    }

    @Test
    void telemetryOptionIsIgnoredOnAPlateau() throws Exception {
        Path input = TestResources.path("simulations/valid/sample.txt");
        assertEquals(0, main().run(new String[]{"--telemetry", "kafka:9092", input.toString()}));
        assertEquals(List.of("The --telemetry option only applies to missions to a planet: ignored."), logs.messages());
        assertEquals(List.of(), sink.telemetry);
    }

    @Test
    void unreachableTelemetryIsReportedBeforeTheMission() throws Exception {
        Path input = TestResources.path("simulations/valid/mini-planet.txt");
        Main main = new Main(TextFileReader::new, out, server -> {
            throw new TelemetryUnavailableException("Kafka does not answer at " + server, null);
        });
        assertEquals(1, main.run(new String[]{"--telemetry", "nowhere:9092", input.toString()}));
        assertEquals(List.of("Cannot send the telemetry: Kafka does not answer at nowhere:9092"), logs.messages());
        assertEquals(List.of(), outputLines());
    }
}
