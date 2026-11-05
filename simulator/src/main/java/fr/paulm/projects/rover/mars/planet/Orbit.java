package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;

// How a planet goes around the Sun: the length of its year in sols, the tilt of its axis (which makes the seasons),
// the power of sunlight above its atmosphere at its mean distance from the Sun, the eccentricity of its orbit
// (0 for a circle) and the season of its perihelion, where it is closest to the Sun (as a solar longitude).
public record Orbit(double yearInSols, double axialTiltDegrees, double solarConstantWattsPerSquareMeter,
                    double eccentricity, double perihelionSolarLongitudeDegrees) {

    public Orbit {
        if (!(yearInSols > 0) || !(axialTiltDegrees >= 0 && axialTiltDegrees <= 90) || !(solarConstantWattsPerSquareMeter >= 0)
                || !(eccentricity >= 0 && eccentricity < 1)) {
            throw new InvalidInputException("Invalid orbit: a year of " + yearInSols + " sols, a tilt of " + axialTiltDegrees
                    + " degrees, sunlight of " + solarConstantWattsPerSquareMeter + " W/m2 and an eccentricity of "
                    + eccentricity);
        }
    }

    // A circular orbit.
    public Orbit(double yearInSols, double axialTiltDegrees, double solarConstantWattsPerSquareMeter) {
        this(yearInSols, axialTiltDegrees, solarConstantWattsPerSquareMeter, 0, 0);
    }

    // Where the planet is on its orbit after this fraction of a year since a given season (solar longitude):
    // its new solar longitude, and its distance to the Sun as a fraction of the mean one (the semi-major axis).
    // The planet goes faster near the Sun (Kepler's second law): the mean anomaly grows at a steady pace, and
    // Kepler's equation (E - e sin E = M) gives the eccentric anomaly, by Newton's method.
    public Position after(double startSolarLongitudeDegrees, double years) {
        double e = eccentricity;
        double startTrue = Math.toRadians(startSolarLongitudeDegrees - perihelionSolarLongitudeDegrees);
        double startEccentric = 2 * Math.atan2(Math.sqrt(1 - e) * Math.sin(startTrue / 2), Math.sqrt(1 + e) * Math.cos(startTrue / 2));
        double mean = startEccentric - e * Math.sin(startEccentric) + 2 * Math.PI * years;
        double eccentric = mean;
        for (int i = 0; i < 30; i++) {
            eccentric -= (eccentric - e * Math.sin(eccentric) - mean) / (1 - e * Math.cos(eccentric));
        }
        double trueAnomaly = 2 * Math.atan2(Math.sqrt(1 + e) * Math.sin(eccentric / 2), Math.sqrt(1 - e) * Math.cos(eccentric / 2));
        double solarLongitude = Math.toDegrees(trueAnomaly) + perihelionSolarLongitudeDegrees;
        return new Position(((solarLongitude % 360) + 360) % 360, 1 - e * Math.cos(eccentric));
    }

    // A place on the orbit: the season, and the distance to the Sun relative to the mean one.
    public record Position(double solarLongitudeDegrees, double relativeDistance) {
    }
}
