package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.MissionResult;
import fr.paulm.projects.rover.domain.simulation.Turn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleReportTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ConsoleReport report = new ConsoleReport(new PrintStream(output, true));
    private final Rover first = new Rover(new Position(0, 2), Direction.N);
    private final Rover second = new Rover(new Position(0, 3), Direction.S);
    private LogCapture warnings;

    @BeforeEach
    void capture() {
        warnings = LogCapture.start(ConsoleReport.class);
    }

    @AfterEach
    void stopCapture() {
        warnings.stop();
    }

    private List<String> outputLines() {
        return output.toString().lines().toList();
    }

    @Test
    void turnListsTheRoversInInputOrder() {
        report.turn(new Turn(2, List.of(first, second), Set.of()));
        assertEquals(List.of("Turn 2: 0 2 N | 0 3 S"), outputLines());
    }

    @Test
    void turnNumbersTheBlockedRoversFromOne() {
        report.turn(new Turn(4, List.of(first, second), Set.of(1, 0)));
        assertEquals(List.of("Turn 4: 0 2 N | 0 3 S (blocked: 1, 2)"), outputLines());
    }

    @Test
    void resultsPrintOneLinePerRoverAndWarnAboutBlockedRovers() {
        report.results(List.of(new MissionResult(first, 0), new MissionResult(second, 3)));
        assertEquals(List.of("0 2 N", "0 3 S"), outputLines());
        assertEquals(List.of("Rover 2: 3 move(s) blocked by another rover."), warnings.messages());
    }
}
