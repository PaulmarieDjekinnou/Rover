package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarsTerrainTest {

    // A small planet: 8 columns and 4 rows on a radius of 10 km, so two rows are about 7.85 km apart.
    // Steps of 500 m: from 0 to "1" rises 500 m (about 3.6 degrees), from 0 to "4" rises 2 km (about 14.3 degrees).
    private final Planet planet = TestPlanets.planet(10_000, 500,
            "00000000",
            "00000000",
            "01400000",
            "00000000");
    private final MarsTerrain terrain = new MarsTerrain(planet, 10);
    private final double rowDistance = 2 * Math.PI * 10_000 / 8;

    @Test
    void slopeIsPositiveUphillAndNegativeDownhill() {
        double expected = Math.toDegrees(Math.atan(500 / rowDistance));
        assertEquals(expected, terrain.slope(new Position(1, 0), new Position(1, 1)), 1e-9);
        assertEquals(-expected, terrain.slope(new Position(1, 1), new Position(1, 0)), 1e-9);
    }

    @Test
    void flatGroundHasNoSlope() {
        assertEquals(0, terrain.slope(new Position(5, 2), new Position(5, 3)));
    }

    @Test
    void aGentleSlopeCanBeDriven() {
        assertTrue(terrain.canDrive(new Position(1, 0), new Position(1, 1)));
    }

    @Test
    void aSteepSlopeCannotBeDrivenUpOrDown() {
        assertFalse(terrain.canDrive(new Position(2, 0), new Position(2, 1)));
        assertFalse(terrain.canDrive(new Position(2, 1), new Position(2, 0)));
    }

    @Test
    void theSameCellCanBeReachedFromOneSideAndNotFromAnother() {
        // The 2 km cell is 1.5 km above its western neighbour, about 7.3 km away (11.7 degrees),
        // and 2 km above its southern neighbour, about 7.9 km away (14.3 degrees)
        MarsTerrain upTo12Degrees = new MarsTerrain(planet, 12);
        assertTrue(upTo12Degrees.canDrive(new Position(1, 1), new Position(2, 1)));
        assertFalse(upTo12Degrees.canDrive(new Position(2, 0), new Position(2, 1)));
    }

    @Test
    void theLimitItselfCanBeDriven() {
        double slope = terrain.slope(new Position(1, 0), new Position(1, 1));
        assertTrue(new MarsTerrain(planet, slope).canDrive(new Position(1, 0), new Position(1, 1)));
    }
}
