package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Position;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

// What the program shows for a mission to a planet: the landings, the turns, the end of each rover's mission
// and the maps on the given output, the warnings about blocked rovers through the logger.
// Rovers are called by their name.
final class MissionReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(MissionReport.class);

    private final PrintStream out;
    private final ReliefMap reliefMap = new ReliefMap(31, 15);

    MissionReport(PrintStream out) {
        this.out = out;
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

    // One line per turn, with the names of the blocked rovers: "Turn 3: 549 341 N | 310 433 E (blocked: Curiosity)".
    void turn(Turn turn, Deployment deployment) {
        String rovers = turn.rovers().stream().map(Object::toString).collect(Collectors.joining(" | "));
        String blocked = turn.blockedRovers().stream().sorted()
                .map(i -> nameOf(deployment.landed().get(i))).collect(Collectors.joining(", "));
        out.println("Turn " + turn.number() + ": " + rovers + (blocked.isEmpty() ? "" : " (blocked: " + blocked + ")"));
    }

    // The end of the mission of each rover that landed, in input order:
    // "Curiosity: 4.54S 137.81E (cell 551 341) facing N, 59.2 km driven in 17.3 sols".
    void results(Deployment deployment, List<MissionResult> results) {
        Planet planet = deployment.planet();
        List<Touchdown> landed = deployment.landed();
        for (int i = 0; i < results.size(); i++) {
            MissionResult result = results.get(i);
            String name = nameOf(landed.get(i));
            if (result.blockedByRovers() > 0) {
                LOGGER.warn("{}: {} move(s) blocked by another rover.", name, result.blockedByRovers());
            }
            if (result.blockedByTerrain() > 0) {
                LOGGER.warn("{}: {} move(s) blocked by the terrain.", name, result.blockedByTerrain());
            }
            out.println(String.format(Locale.ROOT, "%s: %s (cell %s) facing %s, %.1f km driven in %.1f sols",
                    name, planet.centerOf(result.finalRover().position()), result.finalRover().position(),
                    result.finalRover().direction(), result.distanceMeters() / 1_000, sols(result.drivingTime())));
        }
    }

    // A map of the relief around each rover that landed, at the end of the mission. Each rover is drawn with the
    // first letter of its name: in capitals where it is, in lower case where it landed.
    void maps(Deployment deployment, List<MissionResult> results) {
        List<Touchdown> landed = deployment.landed();
        Map<Position, Character> marks = new HashMap<>();
        for (int i = 0; i < results.size(); i++) {
            char initial = nameOf(landed.get(i)).charAt(0);
            marks.putIfAbsent(landed.get(i).cell(), Character.toLowerCase(initial));
            marks.put(results.get(i).finalRover().position(), Character.toUpperCase(initial));
        }
        for (int i = 0; i < results.size(); i++) {
            out.println();
            out.println("Around " + nameOf(landed.get(i)) + ":");
            reliefMap.draw(deployment.planet(), results.get(i).finalRover().position(), marks).forEach(out::println);
        }
    }

    private String nameOf(Touchdown touchdown) {
        return touchdown.plan().name();
    }

    private double sols(Duration duration) {
        return duration.toMillis() / (double) Mars.SOL.toMillis();
    }
}
