package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PowerSourceTest {

    private final Planet planet = TestPlanets.planet(10_000, 100, "00", "00");
    // Noon on the prime meridian at the equinox, under a clear sky
    private final Sun sun = new Sun(planet, 0, 0, 0.5);
    private final Coordinates noon = new Coordinates(0, 0);
    private final Coordinates midnight = new Coordinates(0, 180);

    @Test
    void aGeneratorGivesTheSamePowerDayAndNight() {
        PowerSource generator = new PowerSource.Generator(110);
        assertEquals(110, generator.watts(sun, noon, Duration.ZERO));
        assertEquals(110, generator.watts(sun, midnight, Duration.ZERO));
    }

    @Test
    void solarPanelsTurnAFifthOfTheSunlightIntoPower() {
        PowerSource panels = new PowerSource.SolarPanels(1.3);
        assertEquals(sun.irradiance(noon, Duration.ZERO) * 1.3 * 0.2, panels.watts(sun, noon, Duration.ZERO), 1e-9);
        assertEquals(0, panels.watts(sun, midnight, Duration.ZERO));
    }

    @Test
    void rejectsImpossibleSources() {
        assertThrows(InvalidInputException.class, () -> new PowerSource.Generator(-1));
        assertThrows(InvalidInputException.class, () -> new PowerSource.SolarPanels(0));
        assertThrows(InvalidInputException.class, () -> new PowerSystem(new PowerSource.Generator(110), 0));
    }

    @Test
    void climbingTakesMorePowerUpToTwiceAtTheSteepestSlope() {
        assertEquals(1, PowerSystem.slopeFactor(-5, 10));
        assertEquals(1, PowerSystem.slopeFactor(0, 10));
        assertEquals(1.5, PowerSystem.slopeFactor(5, 10));
        assertEquals(2, PowerSystem.slopeFactor(10, 10));
    }

    @Test
    void heatersTakeMoreWhenTheSunStaysLow() {
        assertEquals(10, PowerSystem.heaterWatts(90), 1e-9);
        assertEquals(40, PowerSystem.heaterWatts(0), 1e-9);
        assertEquals(40, PowerSystem.heaterWatts(-10), 1e-9);
        assertEquals(25, PowerSystem.heaterWatts(30), 1e-9);
    }
}
