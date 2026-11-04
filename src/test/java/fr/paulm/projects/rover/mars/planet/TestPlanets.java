package fr.paulm.projects.rover.mars.planet;

import java.util.List;

// Small planets drawn in text for tests (see ElevationMap.fromDigits): one line per row, from north to south
// as on a map, one digit per cell, the elevation in steps of the given height.
public final class TestPlanets {

    private TestPlanets() {
    }

    public static Planet planet(double radiusMeters, int metersPerDigit, String... northToSouth) {
        return new Planet("test", radiusMeters, ElevationMap.fromDigits(List.of(northToSouth), metersPerDigit));
    }
}
