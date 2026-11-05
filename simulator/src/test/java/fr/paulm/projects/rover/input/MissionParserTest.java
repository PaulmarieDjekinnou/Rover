package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.mission.Ellipse;
import fr.paulm.projects.rover.mars.mission.LandingDate;
import fr.paulm.projects.rover.mars.mission.LandingRules;
import fr.paulm.projects.rover.mars.mission.LandingSite;
import fr.paulm.projects.rover.mars.mission.Operations;
import fr.paulm.projects.rover.mars.mission.PlanetMission;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import fr.paulm.projects.rover.mars.mission.Sky;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.rover.Mobility;
import fr.paulm.projects.rover.mars.rover.PowerSource;
import fr.paulm.projects.rover.mars.rover.PowerSystem;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionParserTest {

    // A flat planet at 0 m: rows centered on 67.5S, 22.5S, 22.5N and 67.5N
    private final Planet flat = TestPlanets.planet(10_000, 100,
            "00000000",
            "00000000",
            "00000000",
            "00000000");
    private final MissionParser parser = new MissionParser(name -> {
        if (!name.equals("flat")) {
            throw new InvalidInputException("Unknown planet: \"" + name + "\"");
        }
        return flat;
    });

    // The settings needed to land on the flat planet, then the given lines
    private PlanetMission parse(String... lines) {
        return parser.parse("planet flat\nlanding-max-elevation 0\nlanding-max-latitude 60\n" + String.join("\n", lines));
    }

    private String error(String... lines) {
        return assertThrows(InvalidInputException.class, () -> parse(lines)).getMessage();
    }

    @Test
    void readsTheSettingsAndTheRoversInOrder() {
        PlanetMission mission = parse("seed 42", "max-slope 15", "speed 0.1", "turn-minutes 2.5",
                "rover One", "site 22.5 67.5", "ellipse 2 1 30", "heading N", "orders LMB",
                "rover Two", "heading E", "site -22.5 10");
        assertSame(flat, mission.planet());
        assertEquals(42, mission.seed());
        assertEquals(new Mobility(0.1, 15, Duration.ofSeconds(150)), mission.mobility());
        assertEquals(new LandingRules(0, 60), mission.landingRules());
        assertEquals(List.of(
                new RoverPlan("One", new LandingSite(new Coordinates(22.5, 67.5), new Ellipse(2, 1, 30)), Direction.N,
                        TestInstructions.of("LMB")),
                new RoverPlan("Two", new LandingSite(new Coordinates(-22.5, 10), Ellipse.NONE), Direction.E, List.of())),
                mission.rovers());
    }

    @Test
    void readsThePowerOfEachRover() {
        PlanetMission mission = parse("rover Solar", "site 22.5 67.5", "heading N", "power solar 1.3", "battery 600",
                "rover Nuclear", "site -22.5 10", "heading N", "power rtg 110",
                "rover Default", "site 22.5 100", "heading N");
        assertEquals(List.of(new PowerSystem(new PowerSource.SolarPanels(1.3), 600),
                        new PowerSystem(new PowerSource.Generator(110), 2_000),
                        PowerSystem.DEFAULT),
                mission.rovers().stream().map(RoverPlan::power).toList());
    }

    @Test
    void readsTheLandingDateOfARover() {
        PlanetMission mission = parse("rover Late", "site 22.5 67.5", "heading N", "landing 3 14:30");
        assertEquals(Optional.of(new LandingDate(3, 14.5)), mission.rovers().getFirst().landingDate());
    }

    @Test
    void readsTheOperations() {
        assertEquals(new Operations(9, 16.5, 250, 2),
                parse("drive-window 09:00 16:30", "max-drive-per-sol 250", "drive-every 2").operations());
        assertEquals(Operations.DEFAULT, parse().operations());
    }

    @Test
    void readsTheWaypointsOfARoverInOrder() {
        PlanetMission mission = parse("rover Explorer", "site 22.5 67.5", "heading N", "goto 22.5 100", "goto -22.5 120");
        assertEquals(List.of(new Coordinates(22.5, 100), new Coordinates(-22.5, 120)), mission.rovers().getFirst().waypoints());
    }

    @Test
    void aRoverCannotHaveBothOrdersAndWaypoints() {
        assertEquals("Line 8: Rover \"Both\" has both orders and waypoints. Expected: one or the other",
                error("rover Both", "site 22.5 67.5", "heading N", "orders M", "goto 22.5 100"));
    }

    @Test
    void aRoverCanHaveItsOwnOperations() {
        PlanetMission mission = parse("drive-window 09:00 16:00",
                "rover Own", "site 22.5 67.5", "heading N", "max-drive-per-sol 40",
                "rover Shared", "site -22.5 10", "heading N");
        assertEquals(Optional.of(new Operations(9, 16, 40, 3)), mission.rovers().get(0).operations());
        assertEquals(Optional.empty(), mission.rovers().get(1).operations());
    }

    @Test
    void aResolutionRefinesTheGridOfThePlanet() {
        // The flat test planet has 8 columns: 8 / 360 is less than a cell per degree
        assertEquals("Line 4: Invalid resolution: the map of this planet has less than one cell per degree",
                error("resolution 64"));
    }

    @Test
    void readsTheSky() {
        PlanetMission mission = parse("start-ls 150", "landing-time 15:30", "dust-opacity 2");
        // A dust opacity given in the file stays the same: no weather
        assertEquals(new Sky(150, 15.5, 2, false), mission.sky());
    }

    @Test
    void settingsHaveDefaults() {
        PlanetMission mission = parser.parse("planet flat\nlanding-max-elevation 0");
        assertEquals(0, mission.seed());
        assertEquals(Mobility.DEFAULT, mission.mobility());
        assertEquals(LandingRules.DEFAULT.withMaxElevation(0), mission.landingRules());
        assertEquals(Sky.DEFAULT, mission.sky());
        assertEquals(List.of(), mission.rovers());
    }

    @Test
    void commentsAndBlankLinesAreIgnored() {
        PlanetMission mission = parser.parse("# A test mission\n\nplanet flat\n# no rover\n\nseed 7\n");
        assertEquals(7, mission.seed());
    }

    @Test
    void acceptsOnlyContentStartingWithAPlanetLine() {
        assertTrue(MissionParser.accepts("# comment\n\nplanet mars\n"));
        assertFalse(MissionParser.accepts("5 5\n1 2 N\nM\n"));
        assertFalse(MissionParser.accepts(""));
    }

    @Test
    void errorsGiveTheLineInTheFile() {
        // The three lines added by parse come first: the rover line is line 4
        assertEquals("Line 5: Unknown rover setting: \"color\". Expected: battery, drive-every, drive-window, ellipse, heading, landing, max-drive-per-sol, orders, power, site",
                error("rover One", "color red"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "altitude 3                         | Line 4: Unknown setting: \"altitude\". Expected: drive-every, drive-window, dust-opacity, landing-max-elevation, landing-max-latitude, landing-time, max-drive-per-sol, max-slope, resolution, rocks, seed, speed, start-ls, turn-minutes",
            "seed 1\\nseed 2                     | Line 5: Repeated setting: \"seed\"",
            "seed one                           | Line 4: Invalid integer: \"one\"",
            "landing-time 25:00                 | Line 4: Invalid time: \"25:00\". Expected: hh:mm, for example 15:00",
            "landing-time 3pm                   | Line 4: Invalid time: \"3pm\". Expected: hh:mm, for example 15:00",
            "drive-window 15:00 10:00          | Line 4: Invalid operations: a drive window from 15.0 to 10.0 hours, 50.0 m per sol, one sol in 3. Expected: a window within the day, a positive distance, and a drive every sol or more",
            "max-drive-per-sol 0               | Line 4: Invalid operations: a drive window from 10.0 to 15.0 hours, 0.0 m per sol, one sol in 3. Expected: a window within the day, a positive distance, and a drive every sol or more",
            "dust-opacity -1                    | Line 4: Invalid dust opacity: -1.0. Expected: a number that is not negative",
            "speed fast                         | Line 4: Invalid number: \"fast\"",
            "speed 0                            | Line 4: Invalid mobility: speed 0.0 m/s, max slope 10.0 degrees, turn time PT5M. Expected: a positive speed, a slope below 90 and a turn time that is not negative",
            "max-slope 90                       | Line 4: Invalid mobility: speed 0.04 m/s, max slope 90.0 degrees, turn time PT5M. Expected: a positive speed, a slope below 90 and a turn time that is not negative",
            "turn-minutes -1                    | Line 4: Invalid mobility: speed 0.04 m/s, max slope 10.0 degrees, turn time PT-1M. Expected: a positive speed, a slope below 90 and a turn time that is not negative",
            "max-slope 10 20                    | Line 4: Expected: max-slope followed by 1 number(s)",
            "rover                              | Line 4: Expected: rover followed by a value",
            "rover A\\nheading N                 | Line 4: Rover \"A\" has no site line",
            "rover A\\nsite 0 0                  | Line 4: Rover \"A\" has no heading line",
            "rover A\\nsite 0 0\\nheading X       | Line 6: Invalid direction: \"X\". Expected: N, E, S or W",
            "rover A\\nsite 0 0\\nheading N\\norders MXM | Line 7: Unknown instruction: \"X\". Expected: L, R, M or B",
            "rover A\\nsite 0\\nheading N         | Line 5: Expected: site followed by 2 number(s)",
            "rover A\\nsite 95 0\\nheading N      | Line 5: Invalid coordinates: latitude 95.0, longitude 0.0. Expected: a latitude between -90 and 90 degrees",
            "rover A\\nsite 70 0\\nheading N      | Line 5: Landing site 70.00N 0.00E is too far from the equator. Expected: a latitude within 60.0 degrees",
            "rover A\\nsite 0 0\\nellipse 1 2 0\\nheading N | Line 6: Invalid landing ellipse: 1.0 x 2.0 km. Expected: a major axis at least as long as the minor axis, which is not negative",
            "rover A\\nsite 0 0\\nheading N\\nrover A\\nsite 0 90\\nheading N | Line 7: Two rovers are named \"A\"",
            "rover A\\nsite 0 0\\nheading N\\npower wind 3 | Line 7: Invalid power: \"wind 3\". Expected: rtg followed by watts, or solar followed by square meters",
            "rover A\\nsite 0 0\\nheading N\\npower rtg lots | Line 7: Invalid number: \"lots\"",
            "rover A\\nsite 0 0\\nheading N\\npower solar 0 | Line 7: Invalid solar panels: 0.0 m2. Expected: a positive area",
            "rover A\\nsite 0 0\\nheading N\\nbattery 0 | Line 7: Invalid battery: 0.0 Wh. Expected: a positive capacity",
            "rover A\\nsite 0 0\\nheading N\\nlanding -1 12:00 | Line 7: Invalid landing: \"-1 12:00\". Expected: a sol from 0 and a time, for example 3 14:00",
            "rover A\\nsite 0 0\\nheading N\\nlanding 2 noon | Line 7: Invalid time: \"noon\". Expected: hh:mm, for example 15:00",
    })
    void invalidMissionsAreRejected(String lines, String message) {
        assertEquals(message, error(lines.replace("\\n", "\n")));
    }

    @Test
    void anUnknownPlanetIsReportedOnThePlanetLine() {
        assertEquals("Line 2: Unknown planet: \"venus\"",
                assertThrows(InvalidInputException.class, () -> parser.parse("\nplanet venus")).getMessage());
    }

    @Test
    void anEmptyMissionIsRejected() {
        assertThrows(InvalidInputException.class, () -> parser.parse("# nothing\n"));
    }
}
