package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.mission.Ellipse;
import fr.paulm.projects.rover.mars.mission.Landing;
import fr.paulm.projects.rover.mars.mission.LandingDate;
import fr.paulm.projects.rover.mars.mission.LandingRules;
import fr.paulm.projects.rover.mars.mission.LandingSite;
import fr.paulm.projects.rover.mars.mission.Operations;
import fr.paulm.projects.rover.mars.mission.PlanetMission;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import fr.paulm.projects.rover.mars.mission.Sky;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.EarthMars;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.rover.Mobility;
import fr.paulm.projects.rover.mars.rover.PowerSource;
import fr.paulm.projects.rover.mars.rover.PowerSystem;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

// The mission format: one keyword and its values per line. Lines starting with # are comments.
//
//   planet mars                   the planet: "mars" (the MOLA map), or "map <file>" (see TextMapParser)
//   seed 42                       optional: the seed of the random draws (0 by default)
//   rocks 0.03                    optional: the share of cells covered by rock fields, which the orbital map does not
//                                 show and the rovers discover on their way (none by default)
//   resolution 64                 optional: the cells per degree, finer than the map (see FineRelief); a multiple
//                                 of the cells per degree of the map (4 for Mars)
//   max-slope 10                  optional: the steepest slope the rovers can drive on, in degrees
//   speed 0.04                    optional: the speed of the rovers on flat ground, in m/s
//   turn-minutes 5                optional: how long a turn on the spot takes, in minutes
//   start-ls 150                  optional: the season at the landing, as a solar longitude in degrees (0 by default)
//   start-date 2012-08-06T05:17   optional, instead of start-ls: the date at the start of the mission, in UTC (a day,
//                                 or a day and a time), which gives the season on Mars
//   landing-time 15:00            optional: the local time of the landing, at the site of the first rover (12:00)
//   dust-opacity 0.5              optional: a dust opacity that stays the same all along (by default, the weather of
//                                 Mars: a dust season and storms on a clear base of 0.5)
//   drive-window 10:00 15:00      optional: the local times between which the rovers drive (10:00 to 15:00)
//   max-drive-per-sol 50          optional: the farthest a rover drives in a drive sol, in meters (50 by default)
//   drive-every 3                 optional: the rovers drive one sol in that many (3 by default)
//   landing-max-elevation -1000   optional: the highest landing site allowed, in meters
//   landing-max-latitude 30       optional: the farthest landing site from the equator, in degrees
//
//   rover Curiosity               then one block per rover, starting with its name
//   site -4.59 137.44             the target: latitude (negative in the south), longitude (0 to 360, to the east)
//   ellipse 20 7 0                optional: major axis and minor axis in km, azimuth in degrees (none by default)
//   heading N                     the direction the rover faces once on the ground
//   orders MMRMM                  optional: the instructions, as in the plateau format
//   power rtg 110                 optional: a radioisotope generator of 110 W, or "solar 1.3" for 1.3 m2 of panels
//   battery 2000                  optional: the capacity of the battery, in Wh (a 110 W generator and 2000 Wh by default)
//   landing 3 14:00               optional: lands on sol 3 of the mission at 14:00 local time (at the start by default)
//   drive-window, max-drive-per-sol   optional: the operations of this rover, if they differ from the mission's
//   goto -4.90 137.40             instead of orders: a waypoint to reach, the route being planned after the landing;
//                                 one line per waypoint, in order
//
// The whole file is read and checked before anything lands. PlanetMission checks the landing sites again
// on construction; they are checked here first, to report the line of the site.
public final class MissionParser {

    private static final Set<String> MISSION_KEYS =
            Set.of("seed", "resolution", "rocks", "max-slope", "speed", "turn-minutes", "landing-max-elevation", "landing-max-latitude",
                    "start-ls", "start-date", "landing-time", "dust-opacity", "drive-window", "max-drive-per-sol", "drive-every");
    private static final Set<String> ROVER_KEYS =
            Set.of("site", "ellipse", "heading", "orders", "power", "battery", "landing", "drive-window",
                    "max-drive-per-sol", "drive-every");

    // A block of lines: its first line (the planet or a rover), then its settings by keyword, and its waypoints
    private record Block(Line first, Map<String, Line> settings, List<Line> waypoints) {
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
        Planet loaded = mission.first().check(() -> planets.apply(mission.first().value("planet")));
        Mobility mobility = mobility(mission.settings());
        LandingRules rules = landingRules(mission.settings());
        long seed = mission.settings().containsKey("seed") ? mission.settings().get("seed").integer() : 0;
        Planet fine = mission.settings().containsKey("resolution")
                ? refined(loaded, mission.settings().get("resolution"), seed) : loaded;
        Planet planet = fine;
        if (mission.settings().containsKey("rocks")) {
            Line line = mission.settings().get("rocks");
            double share = line.decimal();
            planet = line.check(() -> fine.withRockFields(share, seed));
        }
        Sky sky = sky(mission.settings(), planet);

        Landing landing = new PlanetMission(planet, mobility, rules, seed, List.of()).landing();
        Operations missionOperations = operations(mission.settings(), Operations.DEFAULT);
        List<RoverPlan> rovers = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Block block : blocks.subList(1, blocks.size())) {
            RoverPlan rover = rover(block, landing, missionOperations);
            if (!names.add(rover.name())) {
                throw block.first().error("Two rovers are named \"" + rover.name() + "\"");
            }
            rovers.add(rover);
        }
        return new PlanetMission(planet, mobility, rules, sky, missionOperations, seed, rovers);
    }

    // The planet block, then one block per rover. Each block holds known settings, each given once.
    private List<Block> blocks(List<Line> lines) {
        List<Block> blocks = new ArrayList<>();
        Map<String, Line> settings = new HashMap<>();
        blocks.add(new Block(lines.getFirst(), settings, new ArrayList<>()));
        for (Line line : lines.subList(1, lines.size())) {
            if (line.keyword().equals("rover")) {
                settings = new HashMap<>();
                blocks.add(new Block(line, settings, new ArrayList<>()));
                continue;
            }
            boolean inRover = blocks.size() > 1;
            if (inRover && line.keyword().equals("goto")) {
                blocks.getLast().waypoints().add(line);
                continue;
            }
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

    // The planet at the resolution asked for, which must be a whole multiple of that of its map.
    private Planet refined(Planet planet, Line line, long seed) {
        long perDegree = line.integer();
        int mapPerDegree = planet.grid().columns() / 360;
        if (mapPerDegree == 0) {
            throw line.error("Invalid resolution: the map of this planet has less than one cell per degree");
        }
        if (perDegree < mapPerDegree || perDegree % mapPerDegree != 0 || perDegree > 512) {
            throw line.error("Invalid resolution: " + perDegree + " cells per degree. Expected: a multiple of "
                    + mapPerDegree + ", up to 512");
        }
        return planet.refined((int) (perDegree / mapPerDegree), seed);
    }

    // The mobility given by the speed, max-slope and turn-minutes settings, each checked on its own line.
    private Mobility mobility(Map<String, Line> settings) {
        Mobility mobility = Mobility.DEFAULT;
        if (settings.containsKey("speed")) {
            Line line = settings.get("speed");
            double speed = line.decimal();
            Mobility current = mobility;
            mobility = line.check(() -> current.withFlatSpeed(speed));
        }
        if (settings.containsKey("max-slope")) {
            Line line = settings.get("max-slope");
            double maxSlope = line.decimal();
            Mobility current = mobility;
            mobility = line.check(() -> current.withMaxSlope(maxSlope));
        }
        if (settings.containsKey("turn-minutes")) {
            Line line = settings.get("turn-minutes");
            Duration turnTime = Duration.ofSeconds(Math.round(line.decimal() * 60));
            Mobility current = mobility;
            mobility = line.check(() -> current.withTurnTime(turnTime));
        }
        return mobility;
    }

    // The drive window and the distance per sol, each checked on its own line, from the given ones.
    private Operations operations(Map<String, Line> settings, Operations base) {
        Operations operations = base;
        if (settings.containsKey("drive-window")) {
            Line line = settings.get("drive-window");
            String[] times = line.value("drive-window").split("\\s+");
            if (times.length != 2) {
                throw line.error("Expected: drive-window followed by two times, for example 10:00 15:00");
            }
            double start = hours(line, times[0]);
            double end = times[1].equals("24:00") ? 24 : hours(line, times[1]);
            Operations current = operations;
            operations = line.check(() -> new Operations(start, end, current.maxMetersPerSol(), current.driveEverySols()));
        }
        if (settings.containsKey("max-drive-per-sol")) {
            Line line = settings.get("max-drive-per-sol");
            double meters = line.decimal();
            Operations current = operations;
            operations = line.check(() -> new Operations(current.windowStartHours(), current.windowEndHours(), meters,
                    current.driveEverySols()));
        }
        if (settings.containsKey("drive-every")) {
            Line line = settings.get("drive-every");
            long every = line.integer();
            Operations current = operations;
            operations = line.check(() -> new Operations(current.windowStartHours(), current.windowEndHours(),
                    current.maxMetersPerSol(), (int) Math.clamp(every, 0, Integer.MAX_VALUE)));
        }
        return operations;
    }

    // The season, the time of the landing and the dust.
    private Sky sky(Map<String, Line> settings, Planet planet) {
        if (settings.containsKey("start-ls") && settings.containsKey("start-date")) {
            throw settings.get("start-date").error("Both start-ls and start-date give the season: keep one of them");
        }
        double startLs = settings.containsKey("start-ls")
                ? settings.get("start-ls").decimal()
                : settings.containsKey("start-date")
                ? new EarthMars(startDate(settings.get("start-date"))).marsSolarLongitudeDegrees()
                : Sky.DEFAULT.startSolarLongitudeDegrees();
        double hours = settings.containsKey("landing-time")
                ? hours(settings.get("landing-time")) : Sky.DEFAULT.landingHours();
        double dust = settings.containsKey("dust-opacity")
                ? settings.get("dust-opacity").decimal() : Sky.DEFAULT.dustOpacity();
        if (settings.containsKey("dust-opacity")) {
            settings.get("dust-opacity").check(() -> new Sky(startLs, hours, dust).sun(planet, new Coordinates(0, 0)));
        }
        // A dust opacity given in the file stays the same all along; otherwise, the weather of Mars
        return new Sky(startLs, hours, dust, !settings.containsKey("dust-opacity"));
    }

    // "2012-08-06", or "2012-08-06T05:17": a date in UTC.
    private Instant startDate(Line line) {
        String value = line.value("start-date");
        try {
            return value.contains("T") ? LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
                    : LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw line.error("Invalid date: \"" + value + "\". Expected: a day in UTC, with or without a time,"
                    + " for example 2012-08-06 or 2012-08-06T05:17");
        }
    }

    // "3 14:00": sol 3 of the mission, at 14:00 local time.
    private LandingDate landingDate(Line line) {
        String[] parts = line.value("landing").split("\\s+");
        int sol;
        try {
            sol = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            sol = -1;
        }
        if (parts.length != 2 || sol < 0) {
            throw line.error("Invalid landing: \"" + line.value("landing") + "\". Expected: a sol from 0 and a time,"
                    + " for example 3 14:00");
        }
        int landingSol = sol;
        double hours = hours(line, parts[1]);
        return line.check(() -> new LandingDate(landingSol, hours));
    }

    // A local time written hh:mm, in hours.
    private double hours(Line line) {
        return hours(line, line.value(line.keyword()));
    }

    private double hours(Line line, String value) {
        String[] parts = value.split(":");
        try {
            if (parts.length == 2 && parts[1].length() == 2) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                if (hours >= 0 && hours < 24 && minutes >= 0 && minutes < 60) {
                    return hours + minutes / 60.0;
                }
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        throw line.error("Invalid time: \"" + value + "\". Expected: hh:mm, for example 15:00");
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

    private RoverPlan rover(Block block, Landing landing, Operations missionOperations) {
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
        Optional<LandingDate> date = settings.containsKey("landing")
                ? Optional.of(landingDate(settings.get("landing"))) : Optional.empty();
        List<Coordinates> waypoints = new ArrayList<>();
        for (Line line : block.waypoints()) {
            double[] point = line.numbers(2);
            waypoints.add(line.check(() -> new Coordinates(point[0], point[1])));
        }
        PowerSystem power = power(settings);
        LandingSite landingSite = new LandingSite(target, ellipse);
        Line planLine = block.waypoints().isEmpty() ? block.first() : block.waypoints().getFirst();
        Optional<Operations> own = settings.containsKey("drive-window") || settings.containsKey("max-drive-per-sol")
                || settings.containsKey("drive-every") ? Optional.of(operations(settings, missionOperations)) : Optional.empty();
        return planLine.check(() -> new RoverPlan(name, landingSite, heading, orders, power, date, waypoints, own));
    }

    // The power source and the battery of a rover, each checked on its own line.
    private PowerSystem power(Map<String, Line> settings) {
        if (!settings.containsKey("power") && !settings.containsKey("battery")) {
            return PowerSystem.DEFAULT;
        }
        PowerSource source = settings.containsKey("power") ? powerSource(settings.get("power")) : PowerSystem.DEFAULT.source();
        double battery = settings.containsKey("battery")
                ? settings.get("battery").decimal() : PowerSystem.DEFAULT.batteryWattHours();
        Line line = settings.getOrDefault("battery", settings.get("power"));
        return line.check(() -> new PowerSystem(source, battery));
    }

    // "rtg 110": a generator of 110 W; "solar 1.3": 1.3 m2 of solar panels.
    private PowerSource powerSource(Line line) {
        String[] parts = line.value("power").split("\\s+");
        if (parts.length != 2 || !(parts[0].equals("rtg") || parts[0].equals("solar"))) {
            throw line.error("Invalid power: \"" + line.value("power") + "\". Expected: rtg followed by watts,"
                    + " or solar followed by square meters");
        }
        double value;
        try {
            value = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            throw line.error("Invalid number: \"" + parts[1] + "\"");
        }
        return line.check(() -> parts[0].equals("rtg") ? new PowerSource.Generator(value) : new PowerSource.SolarPanels(value));
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
