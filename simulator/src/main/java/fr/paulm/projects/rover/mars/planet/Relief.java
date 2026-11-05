package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.Position;

// The height of the ground in each cell of a latitude / longitude grid, in meters above the reference surface:
// a map read from data (ElevationMap), or one derived from it at a finer resolution (FineRelief).
public interface Relief {

    int columns();

    int rows();

    int elevation(Position cell);
}
