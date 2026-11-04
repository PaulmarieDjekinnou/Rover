package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Terrain;

import java.time.Duration;

// The ground of a planet, for a rover with a given mobility.
// The slope of a move is computed from the elevation of the two cells and the distance between their centers,
// so the same cell can be reachable from one side and not from another. The distance is the one along the surface;
// the slope makes the drive slower, not longer.
public final class MarsTerrain implements Terrain {
    private final Planet planet;
    private final Mobility mobility;

    public MarsTerrain(Planet planet, Mobility mobility) {
        this.planet = planet;
        this.mobility = mobility;
    }

    // The slope of the move in degrees: positive uphill, negative downhill.
    public double slope(Position from, Position to) {
        double rise = planet.elevation(to) - planet.elevation(from);
        return Math.toDegrees(Math.atan2(rise, planet.distance(from, to)));
    }

    // The limit applies both ways: a rover cannot climb a steep slope, and must not go down one either.
    @Override
    public boolean canDrive(Position from, Position to) {
        return Math.abs(slope(from, to)) <= mobility.maxSlopeDegrees();
    }

    @Override
    public double distance(Position from, Position to) {
        return planet.distance(from, to);
    }

    @Override
    public Duration driveTime(Position from, Position to) {
        double seconds = distance(from, to) / mobility.speed(slope(from, to));
        return Duration.ofSeconds(Math.round(seconds));
    }

    @Override
    public Duration turnTime() {
        return mobility.turnTime();
    }
}
