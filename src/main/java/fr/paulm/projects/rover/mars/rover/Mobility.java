package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;

import java.time.Duration;

// How a rover drives (the same for all the rovers of a mission): its speed on flat ground, the steepest slope it can drive on, and the time a turn takes.
public record Mobility(double flatSpeedMetersPerSecond, double maxSlopeDegrees, Duration turnTime) {

    // About the top speed of Curiosity (4 cm/s). The slope limit is low because the 4 cells per degree map
    // smooths slopes over about 15 km: only the strongest relief goes beyond 10 degrees at that scale.
    public static final Mobility DEFAULT = new Mobility(0.04, 10, Duration.ofMinutes(5));

    public Mobility {
        if (!(flatSpeedMetersPerSecond > 0) || !(maxSlopeDegrees >= 0 && maxSlopeDegrees < 90)) {
            throw new InvalidInputException("Invalid mobility: speed " + flatSpeedMetersPerSecond
                    + " m/s, max slope " + maxSlopeDegrees + " degrees. Expected: a positive speed and a slope below 90");
        }
    }

    public Mobility withFlatSpeed(double metersPerSecond) {
        return new Mobility(metersPerSecond, maxSlopeDegrees, turnTime);
    }

    public Mobility withMaxSlope(double degrees) {
        return new Mobility(flatSpeedMetersPerSecond, degrees, turnTime);
    }

    // The speed on a slope (in degrees, positive uphill). Climbing slows the rover down, linearly, down to
    // half its speed at the steepest slope it can take. Going down is as fast as on flat ground.
    public double speed(double slopeDegrees) {
        if (slopeDegrees <= 0) {
            return flatSpeedMetersPerSecond;
        }
        return flatSpeedMetersPerSecond * (1 - 0.5 * Math.min(slopeDegrees / maxSlopeDegrees, 1));
    }
}
