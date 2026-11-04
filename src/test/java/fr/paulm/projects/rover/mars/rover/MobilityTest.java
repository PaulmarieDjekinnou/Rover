package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobilityTest {

    private final Mobility mobility = new Mobility(0.04, 10, Duration.ofMinutes(5));

    @ParameterizedTest
    @CsvSource({"-20, 0.04", "0, 0.04", "5, 0.03", "10, 0.02", "15, 0.02"})
    void climbingSlowsDownToHalfTheSpeedAtTheSteepestSlope(double slope, double expected) {
        assertEquals(expected, mobility.speed(slope), 1e-12);
    }

    @ParameterizedTest
    @CsvSource({"0, 10, 0", "-1, 10, 0", "0.04, -1, 0", "0.04, 90, 0", "0.04, 10, -1"})
    void rejectsAnImpossibleMobility(double speed, double maxSlope, long turnSeconds) {
        assertThrows(InvalidInputException.class, () -> new Mobility(speed, maxSlope, Duration.ofSeconds(turnSeconds)));
    }

    @Test
    void withersChangeOneValue() {
        assertEquals(new Mobility(0.1, 10, Duration.ofMinutes(5)), mobility.withFlatSpeed(0.1));
        assertEquals(new Mobility(0.04, 20, Duration.ofMinutes(5)), mobility.withMaxSlope(20));
        assertEquals(new Mobility(0.04, 10, Duration.ofMinutes(2)), mobility.withTurnTime(Duration.ofMinutes(2)));
    }
}
