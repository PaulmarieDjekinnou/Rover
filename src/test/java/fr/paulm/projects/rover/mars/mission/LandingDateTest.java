package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Mars;
import fr.paulm.projects.rover.mars.planet.Sun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static fr.paulm.projects.rover.mars.mission.LandingTest.PLANET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LandingDateTest {

    // Noon on the prime meridian at the start of the mission
    private final Sun sun = new Sun(PLANET, 0, 0, 0.5);

    @ParameterizedTest
    @CsvSource({"0, 12, 0, 0", "0, 18, 0, 0.25", "0, 6, 0, 0.75", "2, 12, 0, 2", "0, 12, 90, 0.75"})
    void landsDuringItsSolWhenItIsThatLocalTimeAtItsSite(int sol, double hours, double longitude, double expectedSols) {
        Duration time = new LandingDate(sol, hours).time(sun, new Coordinates(0, longitude), Mars.SOL);
        assertEquals(expectedSols * Mars.SOL.toMillis(), time.toMillis(), 1);
        assertEquals(hours, sun.localTime(new Coordinates(0, longitude), time), 1e-6);
    }

    @Test
    void rejectsADateBeforeTheMissionOrAnImpossibleTime() {
        assertThrows(InvalidInputException.class, () -> new LandingDate(-1, 12));
        assertThrows(InvalidInputException.class, () -> new LandingDate(0, 24));
    }
}
