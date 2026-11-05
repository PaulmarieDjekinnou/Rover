package fr.paulm.projects.rover.mars.planet;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DustWeatherTest {

    private final Planet mars = TestPlanets.planet(10_000, 100, "00", "00");

    private Duration year(int year) {
        return Duration.ofMillis(Math.round(year * 668.6 * Mars.SOL.toMillis()));
    }

    @Test
    void theSkyIsClearInTheNorthernSpringAndDustierInTheDustSeason() {
        // Seasons where no storm can be: before Ls 180, and the height of the season is 270
        DustWeather weather = new DustWeather(0.5, mars, 1);
        assertEquals(0.5, weather.opticalDepth(Duration.ZERO, 90), 1e-9);
        assertTrue(weather.opticalDepth(Duration.ZERO, 270) >= 1);
    }

    @Test
    void theSameSeedGivesTheSameStorms() {
        DustWeather first = new DustWeather(0.5, mars, 42);
        DustWeather second = new DustWeather(0.5, mars, 42);
        for (int ls = 180; ls < 360; ls += 5) {
            assertEquals(first.opticalDepth(year(3), ls), second.opticalDepth(year(3), ls));
        }
    }

    @Test
    void someYearsHaveAGlobalStorm() {
        // Over ten years, at least one season goes above 4, the opacity of a global storm
        DustWeather weather = new DustWeather(0.5, mars, 7);
        boolean global = false;
        for (int year = 0; year < 10 && !global; year++) {
            for (int ls = 180; ls < 360 && !global; ls++) {
                global = weather.opticalDepth(year(year).plusSeconds(1), ls) > 4;
            }
        }
        assertTrue(global);
    }
}
