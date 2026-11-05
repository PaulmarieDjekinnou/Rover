package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;

// How a planet goes around the Sun: the length of its year in sols, the tilt of its axis (which makes the seasons),
// and the power of sunlight above its atmosphere, at its mean distance from the Sun.
public record Orbit(double yearInSols, double axialTiltDegrees, double solarConstantWattsPerSquareMeter) {

    public Orbit {
        if (!(yearInSols > 0) || !(axialTiltDegrees >= 0 && axialTiltDegrees <= 90) || !(solarConstantWattsPerSquareMeter >= 0)) {
            throw new InvalidInputException("Invalid orbit: a year of " + yearInSols + " sols, a tilt of " + axialTiltDegrees
                    + " degrees and sunlight of " + solarConstantWattsPerSquareMeter + " W/m2");
        }
    }
}
