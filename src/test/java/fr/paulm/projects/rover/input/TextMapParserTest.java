package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Planet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextMapParserTest {

    private final TextMapParser parser = new TextMapParser();

    @Test
    void readsTheRadiusAndTheRowsFromNorthToSouth() {
        Planet planet = parser.parse("mini", "radius-km 10\nmeters-per-digit 500\n0012\n3000\n");
        assertEquals("mini", planet.name());
        assertEquals(10_000, planet.radiusMeters());
        assertEquals(4, planet.grid().columns());
        assertEquals(2, planet.grid().rows());
        assertEquals(1000, planet.elevation(new Position(3, 1)));
        assertEquals(1500, planet.elevation(new Position(0, 0)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "radius-km 10\\nmeters-per-digit 500                  | Empty map: expected radius-km, meters-per-digit, then the rows of the map",
            "radius 10\\nmeters-per-digit 500\\n00               | Line 1: Expected: radius-km followed by a number",
            "radius-km -1\\nmeters-per-digit 500\\n00            | Line 1: Invalid radius: expected a positive number of km",
            "radius-km 10\\nmeters-per-digit high\\n00           | Line 2: Invalid number: \"high\"",
            "radius-km 10\\nmeters-per-digit 500\\n00\\n000      | Line 4: Invalid map row: \"000\". Expected: 2 digits",
            "radius-km 10\\nmeters-per-digit 500\\n0x            | Line 3: Invalid map row: \"0x\". Expected: 2 digits",
            "radius-km 10\\nmeters-per-digit 500\\n000           | Line 3: Invalid sphere grid: 3 columns and 1 rows. Expected: an even number of columns and at least one row",
    })
    void invalidMapsAreRejected(String content, String message) {
        assertEquals(message, assertThrows(InvalidInputException.class,
                () -> parser.parse("map", content.replace("\\n", "\n"))).getMessage());
    }
}
