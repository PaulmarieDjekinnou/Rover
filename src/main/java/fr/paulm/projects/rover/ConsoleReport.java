package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.MissionResult;
import fr.paulm.projects.rover.domain.Turn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

// What the program shows: the turns and the final positions on the given output (the standard output
// in the program, a buffer in tests), the warnings about blocked rovers through the logger.
// Rovers are numbered from 1, in input order.
final class ConsoleReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleReport.class);

    private final PrintStream out;

    ConsoleReport(PrintStream out) {
        this.out = out;
    }

    // One line per turn: "Turn 3: 0 2 N | 0 3 S (blocked: 1)".
    void turn(Turn turn) {
        String rovers = turn.rovers().stream().map(Object::toString).collect(Collectors.joining(" | "));
        String blocked = turn.blockedRovers().stream().sorted()
                .map(i -> String.valueOf(i + 1)).collect(Collectors.joining(", "));
        out.println("Turn " + turn.number() + ": " + rovers + (blocked.isEmpty() ? "" : " (blocked: " + blocked + ")"));
    }

    // The final positions are the result of the program, one line per rover.
    // A warning reports each rover that had moves blocked by another rover.
    void results(List<MissionResult> results) {
        for (int i = 0; i < results.size(); i++) {
            MissionResult result = results.get(i);
            if (result.blockedMoves() > 0) {
                LOGGER.warn("Rover {}: {} move(s) blocked by another rover.", i + 1, result.blockedMoves());
            }
            out.println(result.finalRover());
        }
    }
}
