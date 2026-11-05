package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunTest {

    // A planet with the day of Mars, on a circular orbit like Mars's (the eccentricity is tested in OrbitTest)
    private final Planet planet = new Planet("circle", 10_000, Mars.SOL, new Orbit(668.6, 25.19, 586),
            ElevationMap.fromDigits(java.util.List.of("00", "00"), 100));
    private final Sun equinox = new Sun(planet, 0, 0, 0);
    private final double precision = 1e-6;

    private Duration sols(double sols) {
        return Duration.ofMillis(Math.round(sols * Mars.SOL.toMillis()));
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 12", "90, 0, 18", "270, 0, 6", "0, 0.5, 0", "0, 0.25, 18"})
    void atTheLandingItIsNoonOnThePrimeMeridian(double longitude, double sols, double expectedHours) {
        assertEquals(expectedHours, equinox.localTime(new Coordinates(0, longitude), sols(sols)), precision);
    }

    @Test
    void noonCanStartOnAnyLongitude() {
        assertEquals(12, new Sun(planet, 0, 137.4, 0).localTime(new Coordinates(-4.6, 137.4), Duration.ZERO), precision);
    }

    @Test
    void theSeasonGoesOnceAroundInAYear() {
        assertEquals(90, equinox.solarLongitude(sols(668.6 / 4)), precision);
        assertEquals(0, equinox.solarLongitude(sols(668.6)), precision);
        assertEquals(10, new Sun(planet, 370, 0, 0).solarLongitude(Duration.ZERO), precision);
    }

    @Test
    void theSunIsAboveTheEquatorAtTheEquinoxAndAboveTheTropicAtTheSolstice() {
        assertEquals(0, equinox.declination(Duration.ZERO), precision);
        assertEquals(25.19, new Sun(planet, 90, 0, 0).declination(Duration.ZERO), precision);
        assertEquals(-25.19, new Sun(planet, 270, 0, 0).declination(Duration.ZERO), precision);
    }

    @Test
    void atNoonOnTheEquinoxTheSunIsAsHighAsTheLatitudeAllows() {
        assertEquals(90, equinox.elevation(new Coordinates(0, 0), Duration.ZERO), precision);
        assertEquals(60, equinox.elevation(new Coordinates(30, 0), Duration.ZERO), precision);
        assertEquals(-90, equinox.elevation(new Coordinates(0, 180), Duration.ZERO), precision);
    }

    @Test
    void sunlightIsWeakenedByTheDust() {
        assertEquals(586, equinox.irradiance(new Coordinates(0, 0), Duration.ZERO), precision);
        assertEquals(586 * Math.exp(-0.5), new Sun(planet, 0, 0, 0.5).irradiance(new Coordinates(0, 0), Duration.ZERO), precision);
        // A low Sun crosses more air: at 30 degrees, twice as much
        assertEquals(586 * 0.5 * Math.exp(-1), new Sun(planet, 0, 0, 0.5).irradiance(new Coordinates(60, 0), Duration.ZERO), 1e-3);
    }

    @Test
    void thereIsNoSunlightAtNight() {
        assertEquals(0, equinox.irradiance(new Coordinates(0, 180), Duration.ZERO));
    }

    @Test
    void theSouthPoleHasNoDayInTheNorthernSummer() {
        Sun northernSummer = new Sun(planet, 90, 0, 0.5);
        for (double hour = 0; hour < 1; hour += 0.05) {
            assertTrue(northernSummer.elevation(new Coordinates(-80, 0), sols(hour)) < 0);
        }
    }

    @Test
    void rejectsANegativeDustOpacity() {
        assertThrows(InvalidInputException.class, () -> new Sun(planet, 0, 0, -1));
    }
}
