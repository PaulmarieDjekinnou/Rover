package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;

import java.time.Duration;

// The dust in the atmosphere: its optical depth at a time of the mission, in a season (solar longitude).
// About 0.5 on a clear day on Mars, above 4 in a global dust storm.
@FunctionalInterface
public interface Dust {

    double opticalDepth(Duration time, double solarLongitudeDegrees);

    // The same dust all the time.
    static Dust constant(double opticalDepth) {
        if (!(opticalDepth >= 0)) {
            throw new InvalidInputException("Invalid dust opacity: " + opticalDepth + ". Expected: a number that is not negative");
        }
        return (time, solarLongitude) -> opticalDepth;
    }
}
