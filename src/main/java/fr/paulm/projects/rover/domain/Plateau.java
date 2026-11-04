package fr.paulm.projects.rover.domain;

public final class Plateau {
    private final int maxX;
    private final int maxY;

    public Plateau(int maxX, int maxY) {
        if (maxX < 0 || maxY < 0) {
            throw new InvalidInputException("Invalid plateau size: \"" + maxX + " " + maxY + "\". Dimensions cannot be negative");
        }
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public boolean contains(Position position) {
        return position.x() >= 0 && position.x() <= maxX
                && position.y() >= 0 && position.y() <= maxY;
    }

    // The plateau is a torus, like a small planet: a position beyond one edge comes back on the
    // opposite edge, on the same row or column. A rover can never leave the plateau.
    public Position wrap(Position position) {
        return new Position(wrap(position.x(), maxX), wrap(position.y(), maxY));
    }

    // floorMod keeps the result positive (-1 becomes max). Computed on long values so that
    // a plateau as wide as Integer.MAX_VALUE, and a coordinate that overflowed by one, still wrap correctly.
    private int wrap(int coordinate, int max) {
        return (int) Math.floorMod((long) coordinate, (long) max + 1);
    }
}
