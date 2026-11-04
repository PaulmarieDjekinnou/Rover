package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.mission.Ellipse;
import fr.paulm.projects.rover.mars.mission.LandingRules;
import fr.paulm.projects.rover.mars.mission.LandingSite;
import fr.paulm.projects.rover.mars.mission.MarsMission;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.rover.Mobility;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarsMissionParserTest {

    // A flat planet at 0 m: rows centered on 67.5S, 22.5S, 22.5N and 67.5N
    private final Planet flat = TestPlanets.planet(10_000, 100,
            "00000000",
            "00000000",
            "00000000",
            "00000000");
    private final MarsMissionParser parser = new MarsMissionParser(name -> {
        if (!name.equals("flat")) {
            throw new InvalidInputException("Unknown planet: \"" + name + "\"");
        }
        return flat;
    });

    // The settings needed to land on the flat planet, then the given lines
    private MarsMission parse(String... lines) {
        return parser.parse("planet flat\nlanding-max-elevation 0\nlanding-max-latitude 60\n" + String.join("\n", lines));
    }

    private String error(String... lines) {
        return assertThrows(InvalidInputException.class, () -> parse(lines)).getMessage();
    }

    @Test
    void readsTheSettingsAndTheRoversInOrder() {
        MarsMission mission = parse("seed 42", "max-slope 15", "speed 0.1",
                "rover One", "site 22.5 67.5", "ellipse 2 1 30", "heading N", "orders LMB",
                "rover Two", "heading E", "site -22.5 10");
        assertSame(flat, mission.planet());
        assertEquals(42, mission.seed());
        assertEquals(new Mobility(0.1, 15, Duration.ofMinutes(5)), mission.mobility());
        assertEquals(new LandingRules(0, 60), mission.landingRules());
        assertEquals(List.of(
                new RoverPlan("One", new LandingSite(new Coordinates(22.5, 67.5), new Ellipse(2, 1, 30)), Direction.N,
                        TestInstructions.of("LMB")),
                new RoverPlan("Two", new LandingSite(new Coordinates(-22.5, 10), Ellipse.NONE), Direction.E, List.of())),
                mission.rovers());
    }

    @Test
    void settingsHaveDefaults() {
        MarsMission mission = parser.parse("planet flat\nlanding-max-elevation 0");
        assertEquals(0, mission.seed());
        assertEquals(Mobility.DEFAULT, mission.mobility());
        assertEquals(LandingRules.DEFAULT.withMaxElevation(0), mission.landingRules());
        assertEquals(List.of(), mission.rovers());
    }

    @Test
    void commentsAndBlankLinesAreIgnored() {
        MarsMission mission = parser.parse("# A test mission\n\nplanet flat\n# no rover\n\nseed 7\n");
        assertEquals(7, mission.seed());
    }

    @Test
    void acceptsOnlyContentStartingWithAPlanetLine() {
        assertTrue(MarsMissionParser.accepts("# comment\n\nplanet mars\n"));
        assertFalse(MarsMissionParser.accepts("5 5\n1 2 N\nM\n"));
        assertFalse(MarsMissionParser.accepts(""));
    }

    @Test
    void errorsGiveTheLineInTheFile() {
        // The three lines added by parse come first: the rover line is line 4
        assertEquals("Line 5: Unknown rover setting: \"color\". Expected: ellipse, heading, orders, site",
                error("rover One", "color red"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "altitude 3                         | Line 4: Unknown setting: \"altitude\". Expected: landing-max-elevation, landing-max-latitude, max-slope, seed, speed",
            "seed 1\\nseed 2                     | Line 5: Repeated setting: \"seed\"",
            "seed one                           | Line 4: Invalid integer: \"one\"",
            "speed fast                         | Line 4: Invalid number: \"fast\"",
            "speed 0                            | Line 4: Invalid mobility: speed 0.0 m/s, max slope 10.0 degrees. Expected: a positive speed and a slope below 90",
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
