package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.MarsTerrain;
import fr.paulm.projects.rover.mars.planet.Mobility;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingTest {

    // A small planet of radius 10 km: rows are 45 degrees (about 7.9 km) apart, centered on 67.5S, 22.5S,
    // 22.5N and 67.5N. Steps of 1 km: a 1 km step between two cells is a 7 degree slope, 9 km is 49 degrees.
    static final Planet PLANET = TestPlanets.planet(10_000, 1_000,
            "00000000",
            "10000000",
            "00090000",
            "00000000");
    static final LandingRules RULES = new LandingRules(500, 60);

    private final Landing landing = new Landing(PLANET, new MarsTerrain(PLANET, Mobility.DEFAULT), RULES);

    private static Coordinates centerOf(int x, int y) {
        return PLANET.centerOf(new Position(x, y));
    }

    @Test
    void acceptsALowFlatSiteNearTheEquator() {
        assertDoesNotThrow(() -> landing.check(centerOf(6, 2)));
    }

    @Test
    void refusesASiteTooHigh() {
        InvalidInputException error = assertThrows(InvalidInputException.class, () -> landing.check(centerOf(0, 2)));
        assertEquals("Landing site 22.50N 22.50E is too high (1000 m). Expected: at most 500 m", error.getMessage());
    }

    @Test
    void refusesASiteTooFarFromTheEquator() {
        InvalidInputException error = assertThrows(InvalidInputException.class, () -> landing.check(centerOf(6, 3)));
        assertEquals("Landing site 67.50N 292.50E is too far from the equator. Expected: a latitude within 60.0 degrees",
                error.getMessage());
    }

    @Test
    void refusesASiteNextToASteepSlope() {
        InvalidInputException error = assertThrows(InvalidInputException.class, () -> landing.check(centerOf(2, 1)));
        assertEquals("Landing site 22.50S 112.50E is too steep: the rover could not drive away in every direction",
                error.getMessage());
    }

    @Test
    void aRoverCannotLeaveTheTopOfACliff() {
        assertFalse(landing.canLeave(new Position(3, 1)));
        assertTrue(landing.canLeave(new Position(2, 1)));
    }

    @Test
    void withoutEllipseTheRoverLandsOnTheTarget() {
        Coordinates target = centerOf(6, 2);
        assertEquals(target, landing.touchdown(new LandingSite(target, Ellipse.NONE), new SplittableRandom(1)));
    }

    @Test
    void theSameSeedGivesTheSameTouchdown() {
        LandingSite site = new LandingSite(centerOf(6, 2), new Ellipse(2, 1, 30));
        assertEquals(landing.touchdown(site, new SplittableRandom(42)), landing.touchdown(site, new SplittableRandom(42)));
    }

    @Test
    void touchdownsStayInTheEllipseAlongItsAzimuth() {
        // A 2 x 1 km ellipse with its major axis east-west: up to 1 km east or west, 500 m north or south
        Coordinates target = new Coordinates(0, 100);
        LandingSite site = new LandingSite(target, new Ellipse(2, 1, 90));
        SplittableRandom random = new SplittableRandom(7);
        double maxNorth = 0;
        double maxEast = 0;
        for (int i = 0; i < 2_000; i++) {
            Coordinates point = landing.touchdown(site, random);
            double north = Math.abs(target.distanceTo(new Coordinates(point.latitude(), target.longitude()), 10_000));
            double east = Math.abs(target.distanceTo(new Coordinates(target.latitude(), point.longitude()), 10_000));
            // Inside the ellipse: (east / 1000)^2 + (north / 500)^2 <= 1
            assertTrue(Math.pow(east / 1_000, 2) + Math.pow(north / 500, 2) <= 1 + 1e-9);
            maxNorth = Math.max(maxNorth, north);
            maxEast = Math.max(maxEast, east);
        }
        // The points fill the ellipse: some of them land near its edges
        assertTrue(maxEast > 950);
        assertTrue(maxNorth > 475);
    }
}
