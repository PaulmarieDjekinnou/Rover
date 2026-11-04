package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Terrain;

// The ground of a planet, for a rover that cannot drive on slopes steeper than a given angle.
// The slope of a move is computed from the elevation of the two cells and the distance between their centers,
// so the same cell can be reachable from one side and not from another.
public final class MarsTerrain implements Terrain {
    private final Planet planet;
    private final double maxSlopeDegrees;

    public MarsTerrain(Planet planet, double maxSlopeDegrees) {
        this.planet = planet;
        this.maxSlopeDegrees = maxSlopeDegrees;
    }

    // The slope of the move in degrees: positive uphill, negative downhill.
    public double slope(Position from, Position to) {
        double rise = planet.elevation(to) - planet.elevation(from);
        return Math.toDegrees(Math.atan2(rise, planet.distance(from, to)));
    }

    // The limit applies both ways: a rover cannot climb a steep slope, and must not go down one either.
    @Override
    public boolean canDrive(Position from, Position to) {
        return Math.abs(slope(from, to)) <= maxSlopeDegrees;
    }
}
