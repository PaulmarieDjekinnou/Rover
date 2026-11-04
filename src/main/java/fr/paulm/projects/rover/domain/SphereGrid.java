package fr.paulm.projects.rover.domain;

// A sphere cut into a latitude / longitude grid, like the maps of Mars.
// x is the column (the longitude, going east) and y the row (the latitude, from the south pole to the north pole),
// so north is still y + 1. Going east or west wraps around the planet, as on a plateau. Going north from the last
// row crosses the north pole: the rover arrives on the same row, on the opposite side of the planet
// (half a turn of longitude away), heading south. The south pole works the same way.
public final class SphereGrid implements Surface {
    private final int columns;
    private final int rows;

    // The number of columns must be even, so that the opposite side of a column is a column too.
    public SphereGrid(int columns, int rows) {
        if (columns < 2 || columns % 2 != 0 || rows < 1) {
            throw new InvalidInputException("Invalid sphere grid: " + columns + " columns and " + rows
                    + " rows. Expected: an even number of columns and at least one row");
        }
        this.columns = columns;
        this.rows = rows;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    @Override
    public boolean contains(Position position) {
        return position.x() >= 0 && position.x() < columns
                && position.y() >= 0 && position.y() < rows;
    }

    @Override
    public Heading next(Position position, Direction direction) {
        Position ahead = direction.ahead(position);
        if (ahead.y() < 0 || ahead.y() >= rows) {
            Position acrossThePole = new Position(Math.floorMod(position.x() + columns / 2, columns), position.y());
            return new Heading(acrossThePole, direction.opposite());
        }
        return new Heading(new Position(Math.floorMod(ahead.x(), columns), ahead.y()), direction);
    }
}
