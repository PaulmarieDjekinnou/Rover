package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanetTest {

    // 8 columns and 4 rows; the first line is the northern row (y = 3)
    private final Planet planet = TestPlanets.planet(10_000, 100,
            "00000000",
            "01200000",
            "00900000",
            "00000005");

    @Test
    void elevationIsReadWithTheNorthernRowFirst() {
        assertEquals(100, planet.elevation(new Position(1, 2)));
        assertEquals(900, planet.elevation(new Position(2, 1)));
        assertEquals(500, planet.elevation(new Position(7, 0)));
    }

    @Test
    void theGridHasTheSizeOfTheMap() {
        assertEquals(8, planet.grid().columns());
        assertEquals(4, planet.grid().rows());
    }

    @Test
    void distanceBetweenTwoRowsIsAnEighthOfTheCircumference() {
        // 4 rows from pole to pole: each row is 45 degrees of latitude
        assertEquals(2 * Math.PI * 10_000 / 8, planet.distance(new Position(0, 1), new Position(0, 2)), 1e-6);
    }

    @Test
    void elevationMapRejectsAWrongNumberOfValues() {
        assertThrows(InvalidInputException.class, () -> new ElevationMap(2, 2, new short[3]));
    }

    @Test
    void mapInDigitsRejectsRowsOfDifferentLengthsOrOtherCharacters() {
        assertThrows(InvalidInputException.class, () -> ElevationMap.fromDigits(List.of("00", "000"), 100));
        assertThrows(InvalidInputException.class, () -> ElevationMap.fromDigits(List.of("0x"), 100));
    }

    @Test
    void theDayMustLastSomeTime() {
        ElevationMap map = new ElevationMap(2, 1, new short[2]);
        assertThrows(InvalidInputException.class, () -> new Planet("still", 10_000, Duration.ZERO, map));
    }

    @Test
    void elevationMapIsACopy() {
        short[] values = {1, 2, 3, 4};
        ElevationMap map = new ElevationMap(2, 2, values);
        values[0] = 99;
        assertEquals(1, map.elevation(new Position(0, 0)));
    }
}
