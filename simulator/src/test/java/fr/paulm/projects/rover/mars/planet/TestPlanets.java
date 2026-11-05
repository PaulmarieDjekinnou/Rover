package fr.paulm.projects.rover.mars.planet;

import java.util.List;

// Small planets drawn in text for tests (see ElevationMap.fromDigits): one line per row, from north to south
// as on a map, one digit per cell, the elevation in steps of the given height. Their day and their orbit are those of Mars.
public final class TestPlanets {

    private TestPlanets() {
    }

    public static Planet planet(double radiusMeters, int metersPerDigit, String... northToSouth) {
        return new Planet("test", radiusMeters, Mars.SOL, Mars.ORBIT, ElevationMap.fromDigits(List.of(northToSouth), metersPerDigit));
    }
}
