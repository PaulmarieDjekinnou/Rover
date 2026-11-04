package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.SphereGrid;

// A planet as the simulation sees it: a sphere of a given radius, cut into the latitude / longitude grid
// of its elevation map. It never changes during a simulation.
public final class Planet {
    private final String name;
    private final double radiusMeters;
    private final ElevationMap elevations;
    private final SphereGrid grid;

    public Planet(String name, double radiusMeters, ElevationMap elevations) {
        this.name = name;
        this.radiusMeters = radiusMeters;
        this.elevations = elevations;
        this.grid = new SphereGrid(elevations.columns(), elevations.rows());
    }

    public String name() {
        return name;
    }

    public double radiusMeters() {
        return radiusMeters;
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
