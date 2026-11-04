package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.simulation.MissionResult;
import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.mars.mission.Batteries;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.RoverPositions;
import fr.paulm.projects.rover.mars.mission.Touchdown;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
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

    // Where each rover touched down, before it starts its orders, and the Sun there: "Curiosity landed at
    // 4.60S 137.45E (cell 549 341), 0.9 km from its target, local time 14:32, Sun 52 degrees high"
    // or "Twin was lost at landing: ...".
    void landings(Deployment deployment) {
        for (Touchdown touchdown : deployment.touchdowns()) {
            if (touchdown.landed()) {
                double fromTarget = touchdown.plan().site().target().distanceTo(touchdown.point(),
                        deployment.planet().radiusMeters());
                out.println(String.format(Locale.ROOT, "%s landed at %s (cell %s), %.1f km from its target, %s",
                        nameOf(touchdown), touchdown.point(), touchdown.cell(), fromTarget / 1_000,
                        sky(deployment.sun(), touchdown.point(), Duration.ZERO)));
            } else {
                out.println(nameOf(touchdown) + " was lost at landing: " + touchdown.failure().orElseThrow().description());
            }
        }
    }

    // One line each time an instruction ends, on the common clock: the time (see clock), then each rover that landed
    // with its position and direction, and the rovers whose move was blocked:
    // "Sol 2 05:12: Polar 67.50N 67.50E N | Climber 22.50S 202.50E S (blocked: Climber)".
    void turn(Turn turn, Deployment deployment, RoverPositions positions) {
        List<Touchdown> landed = deployment.landed();
        List<String> rovers = new ArrayList<>();
        for (int i = 0; i < turn.rovers().size(); i++) {
            rovers.add(nameOf(landed.get(i)) + " " + positions.of(i) + " " + turn.rovers().get(i).direction());
        }
        String blocked = turn.blockedRovers().stream().sorted()
                .map(i -> nameOf(landed.get(i))).collect(Collectors.joining(", "));
        out.println(clock(turn.time(), deployment.planet()) + ": " + String.join(" | ", rovers)
                + (blocked.isEmpty() ? "" : " (blocked: " + blocked + ")"));
    }

    // The end of the mission of each rover that landed, in input order: its exact position (see RoverPositions),
    // its cell and direction, the distance and the time driving, the time waiting for energy, the charge of its
    // battery, and the Sun when it ends: "Curiosity: 4.54S 137.81E (cell 551 341) facing N, 59.2 km driven in
    // 17.3 sols, 6.1 sols waiting for energy, battery 63%, local time 03:12, night".
    // A rover that ran out of energy says so: "Spirit stopped, out of energy: ...".
    void results(Deployment deployment, List<MissionResult> results, RoverPositions positions, Batteries batteries) {
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
            out.println(String.format(Locale.ROOT,
                    "%s%s %s (cell %s) facing %s, %.1f km driven in %.1f sols, %.1f sols waiting for energy, battery %.0f%%, %s",
                    name, result.stopped() ? " stopped, out of energy:" : ":", positions.of(i),
                    result.finalRover().position(), result.finalRover().direction(), result.distanceMeters() / 1_000,
                    sols(result.drivingTime(), planet), sols(result.waitingTime(), planet), 100 * batteries.charge(i),
                    sky(deployment.sun(), positions.of(i), result.totalTime())));
        }
    }

    // A map of the relief around each rover that landed, at the end of the mission. Each rover is drawn with its
    // own letter (see labels): in capitals where it is, in lower case where it landed, and ~ on the cells it drove
    // through.
    void maps(Deployment deployment, List<MissionResult> results, RoverTracks tracks) {
        List<Touchdown> landed = deployment.landed();
        List<Character> labels = labels(landed);
        Map<Position, Character> marks = new HashMap<>();
        for (int i = 0; i < results.size(); i++) {
            tracks.of(i).forEach(cell -> marks.put(cell, '~'));
        }
        for (int i = 0; i < results.size(); i++) {
            marks.put(landed.get(i).cell(), Character.toLowerCase(labels.get(i)));
        }
        for (int i = 0; i < results.size(); i++) {
            marks.put(results.get(i).finalRover().position(), labels.get(i));
        }
        List<String> legend = new ArrayList<>();
        for (int i = 0; i < landed.size(); i++) {
            legend.add(labels.get(i) + " " + nameOf(landed.get(i)));
        }
        for (int i = 0; i < results.size(); i++) {
            out.println();
            out.println("Around " + nameOf(landed.get(i)) + " (" + String.join(", ", legend)
                    + "; lower case where they landed, ~ where they drove):");
            reliefMap.draw(deployment.planet(), results.get(i).finalRover().position(), marks).forEach(out::println);
        }
    }

    // A label for each rover, different from the others: the first letter of its name that no rover before it uses,
    // or else the first free letter of the alphabet, then the first free digit. Beyond 36 rovers, labels run out
    // and the next rovers are all drawn as ?.
    static List<Character> labels(List<Touchdown> rovers) {
        List<Character> labels = new ArrayList<>();
        for (Touchdown rover : rovers) {
            String letters = nameOf(rover).toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
            char label = (char) (letters + "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").chars()
                    .filter(c -> !labels.contains((char) c))
                    .findFirst().orElse('?');
            labels.add(label);
        }
        return labels;
    }

    private static String nameOf(Touchdown touchdown) {
        return touchdown.plan().name();
    }

    // The time since the landing as missions give it: the sol (from 0), then the time of day in hours and minutes
    // of the planet, a sol being cut into 24 hours, like the local time on Mars. "Sol 2 05:12".
    private String clock(Duration time, Planet planet) {
        long day = planet.dayLength().toMillis();
        long sol = time.toMillis() / day;
        long minutes = (time.toMillis() % day) * 24 * 60 / day;
        return String.format(Locale.ROOT, "Sol %d %02d:%02d", sol, minutes / 60, minutes % 60);
    }

    // The local solar time at a point, and how high the Sun is: "local time 14:32, Sun 52 degrees high",
    // or "local time 03:12, night".
    private String sky(Sun sun, Coordinates point, Duration time) {
        double hours = sun.localTime(point, time);
        long minutes = (long) (hours * 60);
        double elevation = sun.elevation(point, time);
        String light = elevation > 0 ? String.format(Locale.ROOT, "Sun %.0f degrees high", elevation) : "night";
        return String.format(Locale.ROOT, "local time %02d:%02d, %s", minutes / 60, minutes % 60, light);
    }

    // A duration in days of the planet (sols).
    private double sols(Duration duration, Planet planet) {
        return duration.toMillis() / (double) planet.dayLength().toMillis();
    }
}
