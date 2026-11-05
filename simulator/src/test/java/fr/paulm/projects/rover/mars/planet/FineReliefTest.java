package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FineReliefTest {

    // Two coarse cells across, 0 m and 1000 m
    private final ElevationMap coarse = ElevationMap.fromDigits(List.of("0909", "0909"), 111);

    @Test
    void theGridIsFactorTimesFiner() {
        FineRelief fine = new FineRelief(coarse, 16, 1);
        assertEquals(64, fine.columns());
        assertEquals(32, fine.rows());
    }

    @Test
    void theLargeScaleIsThatOfTheCoarseRelief() {
        FineRelief fine = new FineRelief(coarse, 16, 1);
        // Near the center of the low and of the high coarse cells: their elevation, within the detail (about 113 m)
        int low = fine.elevation(new Position(8, 8));
        int high = fine.elevation(new Position(24, 8));
        assertTrue(Math.abs(low) < 120, "low: " + low);
        assertTrue(Math.abs(high - 999) < 120, "high: " + high);
        // Halfway, about halfway up
        int middle = fine.elevation(new Position(16, 8));
        assertTrue(middle > 300 && middle < 700, "middle: " + middle);
    }

    @Test
    void theSameSeedGivesTheSameDetailAndAnotherSeedAnotherOne() {
        Position cell = new Position(5, 5);
        assertEquals(new FineRelief(coarse, 16, 1).elevation(cell), new FineRelief(coarse, 16, 1).elevation(cell));
        assertNotEquals(new FineRelief(coarse, 16, 1).elevation(cell), new FineRelief(coarse, 16, 2).elevation(cell));
    }
}
