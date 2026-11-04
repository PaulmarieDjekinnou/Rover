package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.ElevationMap;
import fr.paulm.projects.rover.mars.planet.Planet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolaFileReaderTest {

    private static Planet mars;
    private final MolaFileReader reader = new MolaFileReader();

    // The real map is 2 MB: read it once for all the tests
    @BeforeAll
    static void readMars() {
        mars = new MolaFileReader().mars();
    }

    private int elevationAt(double latitude, double longitude) {
        return mars.elevation(mars.cellAt(new Coordinates(latitude, longitude)));
    }

    @Test
    void readsBigEndianValuesWithTheNorthernLineFirst() throws Exception {
        // Two lines of two values: north 1, -2; south 300, 0x7FFF
        byte[] bytes = {0, 1, (byte) 0xFF, (byte) 0xFE, 1, 44, 0x7F, (byte) 0xFF};
        ElevationMap map = reader.read(new ByteArrayInputStream(bytes), 2, 2);
        assertEquals(1, map.elevation(new Position(0, 1)));
        assertEquals(-2, map.elevation(new Position(1, 1)));
        assertEquals(300, map.elevation(new Position(0, 0)));
        assertEquals(32767, map.elevation(new Position(1, 0)));
    }

    @Test
    void rejectsAFileOfTheWrongSize() {
        assertThrows(InvalidInputException.class, () -> reader.read(new ByteArrayInputStream(new byte[6]), 2, 2));
    }

    @Test
    void marsIsTheWholePlanetAtFourCellsPerDegree() {
        assertEquals(1440, mars.grid().columns());
        assertEquals(720, mars.grid().rows());
    }

    // Known places, with elevations smoothed over cells of about 15 km
    @Test
    void olympusMonsIsMoreThan20KmHigh() {
        assertTrue(elevationAt(18.65, 226.2) > 20_000);
    }

    @Test
    void theFloorOfHellasIsMoreThan6KmDeep() {
        assertTrue(elevationAt(-42.4, 70.5) < -6_000);
    }

    @Test
    void galeCraterWhereCuriosityLandedIsLow() {
        assertTrue(elevationAt(-4.59, 137.44) < -4_000);
    }

    @Test
    void theHighestCellIsOnOlympusMons() {
        Position highest = new Position(0, 0);
        for (int y = 0; y < mars.grid().rows(); y++) {
            for (int x = 0; x < mars.grid().columns(); x++) {
                if (mars.elevation(new Position(x, y)) > mars.elevation(highest)) {
                    highest = new Position(x, y);
                }
            }
        }
        Coordinates summit = mars.centerOf(highest);
        assertEquals(21_134, mars.elevation(highest));
        assertEquals(18.5, summit.latitude(), 1.5);
        assertEquals(226.5, summit.longitude(), 1.5);
    }
}
