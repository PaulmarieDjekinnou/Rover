package fr.paulm.projects.rover.mars.planet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthMarsTest {

    // The landings of Curiosity and Perseverance, at the season NASA gives for them
    @ParameterizedTest
    @CsvSource({"2012-08-06T05:17:00Z, 150.6", "2021-02-18T20:55:00Z, 5.2"})
    void theSeasonOnMarsAtALanding(String date, double solarLongitude) {
        assertEquals(solarLongitude, new EarthMars(Instant.parse(date)).marsSolarLongitudeDegrees(), 0.5);
    }

    @Test
    void atTheClosestApproachOf2003LightTakesThreeMinutes() {
        EarthMars closest = new EarthMars(Instant.parse("2003-08-27T10:00:00Z"));
        // 0.3727 astronomical units
        assertEquals(55.76e9, closest.distanceMeters(), 0.1e9);
        assertEquals(186, closest.lightDelay().toSeconds(), 2);
        assertTrue(closest.sunAngleDegrees() > 170);
    }

    @Test
    void atTheSolarConjunctionOf2023TheSunIsBetweenThePlanets() {
        EarthMars conjunction = new EarthMars(Instant.parse("2023-11-18T00:00:00Z"));
        assertTrue(conjunction.sunAngleDegrees() < 1);
        assertEquals(21, conjunction.lightDelay().toMinutes(), 1);
    }

    @Test
    void mostOfTheTimeTheSunIsFarFromMarsInTheSky() {
        Instant start = Instant.parse("2023-01-01T00:00:00Z");
        int nearTheSun = 0;
        for (int day = 0; day < 780; day++) {
            if (new EarthMars(start.plus(Duration.ofDays(day))).sunAngleDegrees() < 2) {
                nearTheSun++;
            }
        }
        // Two weeks around each conjunction, one every 26 months
        assertEquals(13, nearTheSun, 2);
    }
}
