package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;

// The height of the ground in each cell of a latitude / longitude grid, in meters above the reference surface
// (the areoid on Mars). Stored as 16-bit integers, like the MOLA files: the whole of Mars at 4 cells per degree
// takes about 2 MB.
public final class ElevationMap {
    private final int columns;
    private final int rows;
    private final short[] elevations;

    // The elevations row by row, from the southern row (y = 0) to the northern one, each row from west to east.
    // The array is copied, so the map cannot change afterwards.
    public ElevationMap(int columns, int rows, short[] elevations) {
        if (columns < 1 || rows < 1 || elevations.length != (long) columns * rows) {
            throw new InvalidInputException("Invalid elevation map: " + elevations.length + " values for "
                    + columns + " columns and " + rows + " rows");
        }
        this.columns = columns;
        this.rows = rows;
        this.elevations = elevations.clone();
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public int elevation(Position cell) {
        return elevations[cell.y() * columns + cell.x()];
    }
}
