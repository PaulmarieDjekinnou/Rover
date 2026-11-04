package fr.paulm.projects.rover.mars.mission;

// The constraints on a landing site, inspired by real missions:
// - low enough for the parachute to have enough atmosphere to slow the rover down
//   (Curiosity needed a site about 1 km below the reference surface);
// - close enough to the equator for energy and temperature.
// The ground around the site must also be gentle enough for the rover: see Landing.
public record LandingRules(double maxElevationMeters, double maxLatitudeDegrees) {

    public static final LandingRules DEFAULT = new LandingRules(-1_000, 30);

    public LandingRules withMaxElevation(double meters) {
        return new LandingRules(meters, maxLatitudeDegrees);
    }

    public LandingRules withMaxLatitude(double degrees) {
        return new LandingRules(maxElevationMeters, degrees);
    }
}
