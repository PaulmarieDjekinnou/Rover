package fr.paulm.projects.rover.mars.planet;

// Small planets drawn in text for tests: one line per row, from north to south as on a map, one digit per cell.
// Each digit is the elevation in steps of the given height: with a step of 500 m, "3" is 1500 m high.
public final class TestPlanets {

    private TestPlanets() {
    }

    public static Planet planet(double radiusMeters, int metersPerDigit, String... northToSouth) {
        int rows = northToSouth.length;
        int columns = northToSouth[0].length();
        short[] elevations = new short[columns * rows];
        for (int line = 0; line < rows; line++) {
            int y = rows - 1 - line;
            for (int x = 0; x < columns; x++) {
                elevations[y * columns + x] = (short) (Character.digit(northToSouth[line].charAt(x), 10) * metersPerDigit);
            }
        }
        return new Planet("test", radiusMeters, new ElevationMap(columns, rows, elevations));
    }
}
