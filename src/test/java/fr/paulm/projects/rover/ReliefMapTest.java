package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReliefMapTest {

    // 8 columns and 6 rows, elevations from 0 to 8 km (steps of 1 km)
    private final Planet planet = TestPlanets.planet(10_000, 1_000,
            "00000000",
            "01234567",
            "00000000",
            "00080000",
            "00000000",
            "00000000");

    @Test
    void drawsTheWindowAroundTheCenterNorthUp() {
        // 5 x 3 cells around (3, 3): rows 4 to 2, columns 1 to 5
        assertEquals(List.of(
                "Relief from 0 m (.) to 8000 m (@), north up, 5 x 3 cells:",
                ":-=+*",
                ".....",
                "..@.."), new ReliefMap(5, 3).draw(planet, new Position(3, 3), Map.of()));
    }

    @Test
    void marksAreDrawnOverTheRelief() {
        assertEquals(List.of(
                "Relief from 0 m (.) to 8000 m (@), north up, 3 x 1 cells:",
                ".R."), new ReliefMap(3, 1).draw(planet, new Position(3, 2), Map.of(new Position(3, 2), 'R')));
    }

    @Test
    void theWindowWrapsAroundInLongitude() {
        // Around column 0: columns 7, 0 and 1 of row 4
        assertEquals(List.of(
                "Relief from 0 m (.) to 7000 m (@), north up, 3 x 1 cells:",
                "@.:"), new ReliefMap(3, 1).draw(planet, new Position(0, 4), Map.of()));
    }

    @Test
    void rowsBeyondAPoleAreLeftOut() {
        // Around the northern row: the row above it does not exist
        assertEquals(3, new ReliefMap(3, 5).draw(planet, new Position(3, 5), Map.of()).size() - 1);
    }

    @Test
    void theWindowIsNeverWiderThanThePlanet() {
        assertEquals("01234567".length(), new ReliefMap(31, 1).draw(planet, new Position(3, 4), Map.of()).get(1).length());
    }
}
