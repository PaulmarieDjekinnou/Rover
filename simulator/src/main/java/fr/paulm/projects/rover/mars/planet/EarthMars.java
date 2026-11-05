package fr.paulm.projects.rover.mars.planet;

import java.time.Duration;
import java.time.Instant;

// Where Earth and Mars are around the Sun at a date (UTC): the season on Mars, the distance between the two planets,
// how long light takes to cross it, and how close Mars looks to the Sun from Earth. Mars follows the formulas of
// Allison and McEwen (2000), those of NASA's Mars24; Earth the low-precision formulas of the Astronomical Almanac.
// Both are good to a fraction of a degree, enough for the light delay and the solar conjunctions.
public record EarthMars(Instant date) {
    private static final Instant J2000 = Instant.parse("2000-01-01T12:00:00Z");
    private static final double ASTRONOMICAL_UNIT_METERS = 149_597_870_700.0;
    private static final double LIGHT_SPEED_METERS_PER_SECOND = 299_792_458.0;

    // The season on Mars: the solar longitude, Ls, in degrees.
    public double marsSolarLongitudeDegrees() {
        double days = daysSinceJ2000();
        double meanAnomaly = Math.toRadians(19.3871 + 0.52402073 * days);
        double fictitiousMeanSun = 270.3871 + 0.524038496 * days;
        double equationOfCenter = (10.691 + 3.0e-7 * days) * Math.sin(meanAnomaly) + 0.623 * Math.sin(2 * meanAnomaly)
                + 0.050 * Math.sin(3 * meanAnomaly) + 0.005 * Math.sin(4 * meanAnomaly) + 0.0005 * Math.sin(5 * meanAnomaly);
        return ((fictitiousMeanSun + equationOfCenter) % 360 + 360) % 360;
    }

    public double distanceMeters() {
        double[] mars = mars();
        double[] earth = earth();
        return Math.sqrt(square(mars[0] - earth[0]) + square(mars[1] - earth[1]) + square(mars[2] - earth[2]))
                * ASTRONOMICAL_UNIT_METERS;
    }

    // From 3 minutes, when Mars is closest, to 22 minutes, on the other side of the Sun.
    public Duration lightDelay() {
        return Duration.ofMillis(Math.round(distanceMeters() / LIGHT_SPEED_METERS_PER_SECOND * 1000));
    }

    // The angle between the Sun and Mars seen from Earth, in degrees: near 0 at a solar conjunction, when the Sun
    // is between the two planets and drowns the radio signals.
    public double sunAngleDegrees() {
        double[] mars = mars();
        double[] earth = earth();
        double[] toMars = {mars[0] - earth[0], mars[1] - earth[1], mars[2] - earth[2]};
        double dot = -(earth[0] * toMars[0] + earth[1] * toMars[1] + earth[2] * toMars[2]);
        double lengths = Math.sqrt(square(earth[0]) + square(earth[1]))
                * Math.sqrt(square(toMars[0]) + square(toMars[1]) + square(toMars[2]));
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot / lengths))));
    }

    // Mars around the Sun, in astronomical units, in the plane of Earth's orbit.
    private double[] mars() {
        double days = daysSinceJ2000();
        double meanAnomaly = Math.toRadians(19.3871 + 0.52402073 * days);
        double radius = 1.52367934 * (1.00436 - 0.09309 * Math.cos(meanAnomaly) - 0.004336 * Math.cos(2 * meanAnomaly)
                - 0.00031 * Math.cos(3 * meanAnomaly) - 0.00003 * Math.cos(4 * meanAnomaly));
        double ls = marsSolarLongitudeDegrees();
        double longitude = Math.toRadians(ls + 85.061 - 0.015 * Math.sin(Math.toRadians(71 + 2 * ls)) - 5.5e-6 * days);
        double latitude = Math.toRadians(1.8497 * Math.sin(Math.toRadians(ls - 144.50)));
        return new double[]{radius * Math.cos(latitude) * Math.cos(longitude),
                radius * Math.cos(latitude) * Math.sin(longitude), radius * Math.sin(latitude)};
    }

    // Earth around the Sun: opposite the Sun seen from Earth.
    private double[] earth() {
        double days = daysSinceJ2000();
        double meanAnomaly = Math.toRadians(357.529 + 0.98560028 * days);
        double meanLongitude = 280.459 + 0.98564736 * days;
        double sunLongitude = meanLongitude + 1.915 * Math.sin(meanAnomaly) + 0.020 * Math.sin(2 * meanAnomaly);
        double radius = 1.00014 - 0.01671 * Math.cos(meanAnomaly) - 0.00014 * Math.cos(2 * meanAnomaly);
        double longitude = Math.toRadians(sunLongitude + 180);
        return new double[]{radius * Math.cos(longitude), radius * Math.sin(longitude), 0};
    }

    private double daysSinceJ2000() {
        return Duration.between(J2000, date).toMillis() / 86_400_000.0;
    }

    private static double square(double value) {
        return value * value;
    }
}
