package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.util.Optional;

// Where a rover touched down, in which cell, and why it is lost if it is.
public record Touchdown(RoverPlan plan, Coordinates point, Position cell, Optional<LandingFailure> failure) {

    public boolean landed() {
        return failure.isEmpty();
    }

    // Where the rover is when it is in the given cell. The simulation moves rovers from cell to cell; the rover
    // keeps the place where it touched down inside its cell (its offset from the center, in meters north and east),
    // so its position stays exact: in its landing cell, it is where it touched down.
    public Coordinates positionIn(Position current, Planet planet) {
        Coordinates center = planet.centerOf(cell);
        double radius = planet.radiusMeters();
        double north = radius * Math.toRadians(point.latitude() - center.latitude());
        double longitudeOffset = ((point.longitude() - center.longitude()) % 360 + 540) % 360 - 180;
        double east = radius * Math.cos(Math.toRadians(center.latitude())) * Math.toRadians(longitudeOffset);
        return planet.centerOf(current).shiftedBy(north, east, radius);
    }
}
