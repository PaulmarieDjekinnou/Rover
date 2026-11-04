package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Coordinates;

import java.util.Optional;

// Where a rover touched down, in which cell, and why it is lost if it is.
public record Touchdown(RoverPlan plan, Coordinates point, Position cell, Optional<LandingFailure> failure) {

    public boolean landed() {
        return failure.isEmpty();
    }

}
