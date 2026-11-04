package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.SphereGrid;

import java.time.Duration;

// A planet as the simulation sees it: a sphere of a given radius, cut into the latitude / longitude grid
// of its elevation map, turning on itself once a day (a sol). It never changes during a simulation.
public final class Planet {
    private final String name;
    private final double radiusMeters;
    private final Duration dayLength;
    private final ElevationMap elevations;
    private final SphereGrid grid;

    public Planet(String name, double radiusMeters, Duration dayLength, ElevationMap elevations) {
        if (!dayLength.isPositive()) {
            throw new InvalidInputException("Invalid day length: " + dayLength + ". Expected: a positive duration");
        }
        this.name = name;
        this.radiusMeters = radiusMeters;
        this.dayLength = dayLength;
        this.elevations = elevations;
        this.grid = new SphereGrid(elevations.columns(), elevations.rows());
    }

    public String name() {
        return name;
    }

    public double radiusMeters() {
        return radiusMeters;
    }

    // The length of a solar day, a sol: 24 h 39 min 35 s on Mars.
    public Duration dayLength() {
        return dayLength;
    }

    public SphereGrid grid() {
        return grid;
    }

    public int elevation(Position cell) {
        return elevations.elevation(cell);
    }

    public Coordinates centerOf(Position cell) {
        return Coordinates.centerOf(cell, grid);
    }

    public Position cellAt(Coordinates point) {
        return point.cellIn(grid);
    }

    // The distance between the centers of two cells, along the surface.
    public double distance(Position from, Position to) {
        return centerOf(from).distanceTo(centerOf(to), radiusMeters);
    }
}
