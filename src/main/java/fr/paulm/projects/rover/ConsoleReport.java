package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.simulation.MissionResult;
import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.Touchdown;
import fr.paulm.projects.rover.mars.planet.Mars;
import fr.paulm.projects.rover.mars.planet.Planet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

// What the program shows: the turns and the final positions on the given output (the standard output
// in the program, a buffer in tests), the warnings about blocked rovers through the logger.
// On a plateau, rovers are numbered from 1, in input order. On a planet, they are called by their name.
final class ConsoleReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleReport.class);

    private final PrintStream out;

    ConsoleReport(PrintStream out) {
        this.out = out;
    }

    // One line per turn: "Turn 3: 0 2 N | 0 3 S (blocked: 1)".
    void turn(Turn turn) {
        turn(turn, i -> String.valueOf(i + 1));
    }

    // The same line on a planet, with the names of the blocked rovers: "Turn 3: 549 341 N | 310 433 E (blocked: Curiosity)".
    void turn(Turn turn, Deployment deployment) {
        turn(turn, i -> nameOf(deployment.landed().get(i)));
    }

    private void turn(Turn turn, IntFunction<String> label) {
        String rovers = turn.rovers().stream().map(Object::toString).collect(Collectors.joining(" | "));
        String blocked = turn.blockedRovers().stream().sorted()
                .map(label::apply).collect(Collectors.joining(", "));
        out.println("Turn " + turn.number() + ": " + rovers + (blocked.isEmpty() ? "" : " (blocked: " + blocked + ")"));
    }

    // The final positions are the result of the program, one line per rover.
    // A warning reports each rover that had moves blocked by another rover, and another one by the terrain.
    void results(List<MissionResult> results) {
        for (int i = 0; i < results.size(); i++) {
            MissionResult result = results.get(i);
            warnAboutBlockedMoves("Rover " + (i + 1), result);
            out.println(result.finalRover());
        }
    }

    // Where each rover touched down, before it starts its orders:
    // "Curiosity landed at 4.60S 137.45E (cell 549 341), 0.9 km from its target" or "Twin was lost at landing: ...".
    void landings(Deployment deployment) {
        for (Touchdown touchdown : deployment.touchdowns()) {
            if (touchdown.landed()) {
                double fromTarget = touchdown.plan().site().target().distanceTo(touchdown.point(),
                        deployment.planet().radiusMeters());
                out.println(String.format(Locale.ROOT, "%s landed at %s (cell %s), %.1f km from its target",
                        nameOf(touchdown), touchdown.point(), touchdown.cell(), fromTarget / 1_000));
            } else {
                out.println(nameOf(touchdown) + " was lost at landing: " + touchdown.failure().orElseThrow().description());
            }
        }
    }

    // The end of the mission of each rover that landed, in input order:
    // "Curiosity: 4.54S 137.81E (cell 551 341) facing N, 59.2 km driven in 17.3 sols".
    void results(Deployment deployment, List<MissionResult> results) {
        Planet planet = deployment.planet();
        List<Touchdown> landed = deployment.landed();
        for (int i = 0; i < results.size(); i++) {
            MissionResult result = results.get(i);
            String name = nameOf(landed.get(i));
            warnAboutBlockedMoves(name, result);
            out.println(String.format(Locale.ROOT, "%s: %s (cell %s) facing %s, %.1f km driven in %.1f sols",
                    name, planet.centerOf(result.finalRover().position()), result.finalRover().position(),
                    result.finalRover().direction(), result.distanceMeters() / 1_000, sols(result.drivingTime())));
        }
    }

    private void warnAboutBlockedMoves(String rover, MissionResult result) {
        if (result.blockedByRovers() > 0) {
            LOGGER.warn("{}: {} move(s) blocked by another rover.", rover, result.blockedByRovers());
        }
        if (result.blockedByTerrain() > 0) {
            LOGGER.warn("{}: {} move(s) blocked by the terrain.", rover, result.blockedByTerrain());
        }
    }

    private String nameOf(Touchdown touchdown) {
        return touchdown.plan().name();
    }

    private double sols(Duration duration) {
        return duration.toMillis() / (double) Mars.SOL.toMillis();
    }
}
