package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.mission.Ellipse;
import fr.paulm.projects.rover.mars.mission.Landing;
import fr.paulm.projects.rover.mars.mission.LandingRules;
import fr.paulm.projects.rover.mars.mission.LandingSite;
import fr.paulm.projects.rover.mars.mission.PlanetMission;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.rover.Mobility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
// The whole file is read and checked before anything lands. PlanetMission checks the landing sites again
// on construction; they are checked here first, to report the line of the site.
public final class MissionParser {

    private static final Set<String> MISSION_KEYS =
            Set.of("seed", "max-slope", "speed", "landing-max-elevation", "landing-max-latitude");
    private static final Set<String> ROVER_KEYS = Set.of("site", "ellipse", "heading", "orders");

    // A block of lines: its first line (the planet or a rover), then its settings by keyword
    private record Block(Line first, Map<String, Line> settings) {
    }

    // Finds the planet named on the planet line ("mars" or "map <file>"): the reader knows where files are
    private final Function<String, Planet> planets;

    public MissionParser(Function<String, Planet> planets) {
        this.planets = planets;
    }

    // Whether the content is in this format: its first line names the planet.
    public static boolean accepts(String content) {
        return content.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .findFirst().map(line -> line.startsWith("planet ")).orElse(false);
    }

    public PlanetMission parse(String content) {
        List<Line> lines = Line.nonBlankLines(content).stream().filter(line -> !line.text().startsWith("#")).toList();
        if (lines.isEmpty()) {
            throw new InvalidInputException("Empty mission: the first line must name the planet, for example \"planet mars\"");
        }
        List<Block> blocks = blocks(lines);
        Block mission = blocks.getFirst();
        Planet planet = mission.first().check(() -> planets.apply(mission.first().value("planet")));
        Mobility mobility = mobility(mission.settings());
        LandingRules rules = landingRules(mission.settings());
        long seed = mission.settings().containsKey("seed") ? mission.settings().get("seed").integer() : 0;

        Landing landing = new PlanetMission(planet, mobility, rules, seed, List.of()).landing();
        List<RoverPlan> rovers = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Block block : blocks.subList(1, blocks.size())) {
            RoverPlan rover = rover(block, landing);
            if (!names.add(rover.name())) {
                throw block.first().error("Two rovers are named \"" + rover.name() + "\"");
            }
            rovers.add(rover);
        }
        return new PlanetMission(planet, mobility, rules, seed, rovers);
    }

    // The planet block, then one block per rover. Each block holds known settings, each given once.
    private List<Block> blocks(List<Line> lines) {
        List<Block> blocks = new ArrayList<>();
        Map<String, Line> settings = new HashMap<>();
        blocks.add(new Block(lines.getFirst(), settings));
        for (Line line : lines.subList(1, lines.size())) {
            if (line.keyword().equals("rover")) {
                settings = new HashMap<>();
                blocks.add(new Block(line, settings));
                continue;
            }
            boolean inRover = blocks.size() > 1;
            Set<String> keys = inRover ? ROVER_KEYS : MISSION_KEYS;
            String kind = inRover ? "rover setting" : "setting";
            if (!keys.contains(line.keyword())) {
                throw line.error("Unknown " + kind + ": \"" + line.keyword() + "\". Expected: "
                        + String.join(", ", keys.stream().sorted().toList()));
            }
            if (settings.put(line.keyword(), line) != null) {
                throw line.error("Repeated " + kind + ": \"" + line.keyword() + "\"");
            }
        }
        return blocks;
    }

    // The mobility given by the speed and max-slope settings. An invalid one is reported on the first of them.
    private Mobility mobility(Map<String, Line> settings) {
        if (!settings.containsKey("speed") && !settings.containsKey("max-slope")) {
            return Mobility.DEFAULT;
        }
        double speed = settings.containsKey("speed")
                ? settings.get("speed").decimal() : Mobility.DEFAULT.flatSpeedMetersPerSecond();
        double maxSlope = settings.containsKey("max-slope")
                ? settings.get("max-slope").decimal() : Mobility.DEFAULT.maxSlopeDegrees();
        Line line = settings.getOrDefault("speed", settings.get("max-slope"));
        return line.check(() -> new Mobility(speed, maxSlope, Mobility.DEFAULT.turnTime()));
    }

    private LandingRules landingRules(Map<String, Line> settings) {
        LandingRules rules = LandingRules.DEFAULT;
        if (settings.containsKey("landing-max-elevation")) {
            rules = rules.withMaxElevation(settings.get("landing-max-elevation").decimal());
        }
        if (settings.containsKey("landing-max-latitude")) {
            rules = rules.withMaxLatitude(settings.get("landing-max-latitude").decimal());
        }
        return rules;
    }

    private RoverPlan rover(Block block, Landing landing) {
        String name = block.first().value("rover");
        Map<String, Line> settings = block.settings();
        for (String required : List.of("site", "heading")) {
            if (!settings.containsKey(required)) {
                throw block.first().error("Rover \"" + name + "\" has no " + required + " line");
            }
        }
        Line siteLine = settings.get("site");
        double[] site = siteLine.numbers(2);
        Coordinates target = siteLine.check(() -> new Coordinates(site[0], site[1]));
        siteLine.check(() -> {
            landing.check(target);
            return target;
        });
        Ellipse ellipse = Ellipse.NONE;
        if (settings.containsKey("ellipse")) {
            Line ellipseLine = settings.get("ellipse");
            double[] axes = ellipseLine.numbers(3);
            ellipse = ellipseLine.check(() -> new Ellipse(axes[0], axes[1], axes[2]));
        }
        Line headingLine = settings.get("heading");
        String code = headingLine.value("heading");
        Direction heading = Direction.fromCode(code)
                .orElseThrow(() -> headingLine.error("Invalid direction: \"" + code + "\". Expected: N, E, S or W"));
        List<Instruction> orders = settings.containsKey("orders") ? orders(settings.get("orders")) : List.of();
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
}
