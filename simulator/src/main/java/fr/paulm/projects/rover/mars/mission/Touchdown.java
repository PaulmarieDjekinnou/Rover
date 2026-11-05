package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Coordinates;

import java.time.Duration;
import java.util.Optional;

// When a rover touched down (on the mission clock), where, in which cell, and why it is lost if it is.
public record Touchdown(RoverPlan plan, Duration time, Coordinates point, Position cell, Optional<LandingFailure> failure) {

    // A rover that touched down at the start of the mission.
    public Touchdown(RoverPlan plan, Coordinates point, Position cell, Optional<LandingFailure> failure) {
        this(plan, Duration.ZERO, point, cell, failure);
    }

    public boolean landed() {
        return failure.isEmpty();
    }
}
