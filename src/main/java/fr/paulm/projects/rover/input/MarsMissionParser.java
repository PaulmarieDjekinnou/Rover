package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.mission.Ellipse;
import fr.paulm.projects.rover.mars.mission.Landing;
import fr.paulm.projects.rover.mars.mission.LandingRules;
import fr.paulm.projects.rover.mars.mission.LandingSite;
import fr.paulm.projects.rover.mars.mission.MarsMission;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Mobility;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

// The mission format: one keyword and its values per line. Lines starting with # are comments.
//
//   planet mars                   the planet: "mars" (the MOLA map), or "map <file>" (see TextMapParser)
//   seed 42                       optional: the seed of the random draws (0 by default)
//   max-slope 10                  optional: the steepest slope the rovers can drive on, in degrees
//   speed 0.04                    optional: the speed of the rovers on flat ground, in m/s
//   landing-max-elevation -1000   optional: the highest landing site allowed, in meters
//   landing-max-latitude 30       optional: the farthest landing site from the equator, in degrees
//
//   rover Curiosity               then one block per rover, starting with its name
//   site -4.59 137.44             the target: latitude (negative in the south), longitude (0 to 360, to the east)
//   ellipse 20 7 0                optional: major axis and minor axis in km, azimuth in degrees (none by default)
//   heading N                     the direction the rover faces once on the ground
//   orders MMRMM                  optional: the instructions, as in the plateau format
//
// The whole file is read and checked before anything lands, landing sites included.
public final class MarsMissionParser {

    private static final Set<String> MISSION_KEYS =
            Set.of("seed", "max-slope", "speed", "landing-max-elevation", "landing-max-latitude");
    private static final Set<String> ROVER_KEYS = Set.of("site", "ellipse", "heading", "orders");

    // Finds the planet named on the planet line ("mars" or "map <file>"): the reader knows where files are
    private final Function<String, Planet> planets;

    public MarsMissionParser(Function<String, Planet> planets) {
        this.planets = planets;
    }

    // Whether the content is in this format: its first line names the planet.
    public static boolean accepts(String content) {
        return content.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .findFirst().map(line -> line.startsWith("planet ")).orElse(false);
    }

    public MarsMission parse(String content) {
        List<Line> lines = Line.nonBlankLines(content).stream().filter(line -> !line.text().startsWith("#")).toList();
        if (lines.isEmpty()) {
            throw new InvalidInputException("Empty mission: the first line must name the planet, for example \"planet mars\"");
        }
        Line planetLine = lines.getFirst();
        Planet planet = planet(planetLine, value(planetLine, "planet"));

        // The mission settings, up to the first rover
        int firstRover = nextRover(lines, 1);
        Map<String, Line> settings = settings(lines.subList(1, firstRover), MISSION_KEYS, "setting");
        Mobility mobility = mobility(settings);
        LandingRules rules = LandingRules.DEFAULT;
        if (settings.containsKey("landing-max-elevation")) {
            rules = rules.withMaxElevation(number(settings.get("landing-max-elevation")));
        }
        if (settings.containsKey("landing-max-latitude")) {
            rules = rules.withMaxLatitude(number(settings.get("landing-max-latitude")));
        }
        long seed = settings.containsKey("seed") ? integer(settings.get("seed")) : 0;
        Landing landing = new MarsMission(planet, mobility, rules, seed, List.of()).landing();

        // The rovers, one block each: its rover line, then its settings up to the next rover
        List<RoverPlan> rovers = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (int start = firstRover; start < lines.size(); ) {
            Line roverLine = lines.get(start);
            String name = value(roverLine, "rover");
            if (!names.add(name)) {
                throw roverLine.error("Two rovers are named \"" + name + "\"");
            }
            int end = nextRover(lines, start + 1);
            rovers.add(rover(roverLine, name, settings(lines.subList(start + 1, end), ROVER_KEYS, "rover setting"), landing));
            start = end;
        }
        return new MarsMission(planet, mobility, rules, seed, rovers);
    }

    // The index of the next rover line from the given one, or the end of the lines.
    private int nextRover(List<Line> lines, int from) {
        int index = from;
        while (index < lines.size() && !keyOf(lines.get(index)).equals("rover")) {
            index++;
        }
        return index;
    }

    // The lines of a block by keyword. Each keyword must be known, and given once.
    private Map<String, Line> settings(List<Line> block, Set<String> keys, String kind) {
        Map<String, Line> settings = new HashMap<>();
        for (Line line : block) {
            String key = keyOf(line);
            if (!keys.contains(key)) {
                throw line.error("Unknown " + kind + ": \"" + key + "\". Expected: "
                        + String.join(", ", keys.stream().sorted().toList()));
            }
            if (settings.put(key, line) != null) {
                throw line.error("Repeated " + kind + ": \"" + key + "\"");
            }
        }
        return settings;
    }

    // The mobility given by the speed and max-slope settings. An invalid one is reported on the first of them.
    private Mobility mobility(Map<String, Line> settings) {
        double speed = settings.containsKey("speed")
                ? number(settings.get("speed")) : Mobility.DEFAULT.flatSpeedMetersPerSecond();
        double maxSlope = settings.containsKey("max-slope")
                ? number(settings.get("max-slope")) : Mobility.DEFAULT.maxSlopeDegrees();
        if (!settings.containsKey("speed") && !settings.containsKey("max-slope")) {
            return Mobility.DEFAULT;
        }
        Line line = settings.getOrDefault("speed", settings.get("max-slope"));
        return rule(line, () -> new Mobility(speed, maxSlope, Mobility.DEFAULT.turnTime()));
    }

    private Planet planet(Line line, String name) {
        try {
            return planets.apply(name);
        } catch (InvalidInputException e) {
            throw line.error(e.getMessage());
        }
    }

    private RoverPlan rover(Line roverLine, String name, Map<String, Line> keys, Landing landing) {
        for (String required : List.of("site", "heading")) {
            if (!keys.containsKey(required)) {
                throw roverLine.error("Rover \"" + name + "\" has no " + required + " line");
            }
        }
        Line siteLine = keys.get("site");
        double[] site = numbers(siteLine, 2);
        Coordinates target = rule(siteLine, () -> new Coordinates(site[0], site[1]));
        try {
            landing.check(target);
        } catch (InvalidInputException e) {
            throw siteLine.error(e.getMessage());
        }
        Ellipse ellipse = Ellipse.NONE;
        if (keys.containsKey("ellipse")) {
            double[] axes = numbers(keys.get("ellipse"), 3);
            ellipse = rule(keys.get("ellipse"), () -> new Ellipse(axes[0], axes[1], axes[2]));
        }
        Line headingLine = keys.get("heading");
        String code = value(headingLine, "heading");
        Direction heading = Direction.fromCode(code)
                .orElseThrow(() -> headingLine.error("Invalid direction: \"" + code + "\". Expected: N, E, S or W"));
        List<Instruction> orders = keys.containsKey("orders") ? orders(keys.get("orders")) : List.of();
        return new RoverPlan(name, new LandingSite(target, ellipse), heading, orders);
    }

    private List<Instruction> orders(Line line) {
        String letters = line.text().substring("orders".length()).trim();
        List<Instruction> orders = new ArrayList<>();
        for (char c : letters.toCharArray()) {
            orders.add(Instruction.fromCode(c)
                    .orElseThrow(() -> line.error("Unknown instruction: \"" + c + "\". Expected: L, R, M or B")));
        }
        return orders;
    }

    // A rule of the domain (coordinates, ellipse, landing site, mobility): add the line number to its message
    private <T> T rule(Line line, Supplier<T> value) {
        try {
            return value.get();
        } catch (InvalidInputException e) {
            throw line.error(e.getMessage());
        }
    }

    private String keyOf(Line line) {
        return line.text().split("\\s+", 2)[0];
    }

    // The rest of the line after its keyword, which must not be empty.
    private String value(Line line, String key) {
        String[] parts = line.text().split("\\s+", 2);
        if (!parts[0].equals(key) || parts.length < 2) {
            throw line.error("Expected: " + key + " followed by a value");
        }
        return parts[1];
    }

    private double number(Line line) {
        return numbers(line, 1)[0];
    }

    private long integer(Line line) {
        String value = value(line, keyOf(line));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw line.error("Invalid integer: \"" + value + "\"");
        }
    }

    private double[] numbers(Line line, int count) {
        String[] parts = line.text().split("\\s+");
        if (parts.length != count + 1) {
            throw line.error("Expected: " + parts[0] + " followed by " + count + " number(s)");
        }
        double[] numbers = new double[count];
        for (int i = 0; i < count; i++) {
            try {
                numbers[i] = Double.parseDouble(parts[i + 1]);
            } catch (NumberFormatException e) {
                throw line.error("Invalid number: \"" + parts[i + 1] + "\"");
            }
        }
        return numbers;
    }
}
