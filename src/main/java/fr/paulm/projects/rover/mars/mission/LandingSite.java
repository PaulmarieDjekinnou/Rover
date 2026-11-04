package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.mars.planet.Coordinates;

// Where a rover is sent: the point it aims at, and the ellipse it lands in.
public record LandingSite(Coordinates target, Ellipse ellipse) {
}
