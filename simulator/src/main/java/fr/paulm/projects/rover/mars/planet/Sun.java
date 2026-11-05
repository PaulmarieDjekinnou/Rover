package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;

import java.time.Duration;

// Where the Sun is in the sky of a planet during a mission, and how much sunlight reaches the ground.
// The time is the time of the mission clock, from the landing:
// - the season is given by the solar longitude (Ls): 0 at the northern spring equinox, 90 at the northern summer
//   solstice. It starts at the given value and goes once around in a year, faster near the Sun on an eccentric
//   orbit (see Orbit.after), and sunlight is stronger there, as the square of the distance;
// - the time of day: at the landing, it is noon on the given longitude, and the planet turns once a sol.
// Sunlight on the ground is weakened by the dust of the atmosphere (see Dust), all the more so as the Sun is low
// and its light crosses more air.
public final class Sun {
    private final Planet planet;
    private final double startSolarLongitudeDegrees;
    private final double startNoonLongitudeDegrees;
    private final Dust dust;

    // Under a dust that does not change.
    public Sun(Planet planet, double startSolarLongitudeDegrees, double startNoonLongitudeDegrees, double opticalDepth) {
        this(planet, startSolarLongitudeDegrees, startNoonLongitudeDegrees, checked(opticalDepth, startSolarLongitudeDegrees));
    }

    public Sun(Planet planet, double startSolarLongitudeDegrees, double startNoonLongitudeDegrees, Dust dust) {
        if (!Double.isFinite(startSolarLongitudeDegrees) || !Double.isFinite(startNoonLongitudeDegrees)) {
            throw new InvalidInputException("Invalid sky: solar longitude " + startSolarLongitudeDegrees
                    + ", noon longitude " + startNoonLongitudeDegrees);
        }
        this.planet = planet;
        this.startSolarLongitudeDegrees = startSolarLongitudeDegrees;
        this.startNoonLongitudeDegrees = startNoonLongitudeDegrees;
        this.dust = dust;
    }

    private static Dust checked(double opticalDepth, double startSolarLongitudeDegrees) {
        if (!(opticalDepth >= 0)) {
            throw new InvalidInputException("Invalid sky: solar longitude " + startSolarLongitudeDegrees
                    + ", dust opacity " + opticalDepth + ". Expected: a dust opacity that is not negative");
        }
        return Dust.constant(opticalDepth);
    }

    // The optical depth of the dust at that time.
    public double opticalDepth(Duration time) {
        return dust.opticalDepth(time, solarLongitude(time));
    }

    // The season, in degrees from 0 to 360.
    public double solarLongitude(Duration time) {
        return onOrbit(time).solarLongitudeDegrees();
    }

    private Orbit.Position onOrbit(Duration time) {
        return planet.orbit().after(startSolarLongitudeDegrees, sols(time) / planet.orbit().yearInSols());
    }

    // The latitude where the Sun is at the zenith at noon: from -tilt to +tilt over the year.
    public double declination(Duration time) {
        double tilt = Math.toRadians(planet.orbit().axialTiltDegrees());
        return Math.toDegrees(Math.asin(Math.sin(tilt) * Math.sin(Math.toRadians(solarLongitude(time)))));
    }

    // The longitude where it is noon: moving west as the planet turns east.
    public double noonLongitude(Duration time) {
        return ((startNoonLongitudeDegrees - 360 * sols(time)) % 360 + 360) % 360;
    }

    // The local solar time at a point, in hours of the planet (a sol cut into 24 hours): 12 at noon.
    public double localTime(Coordinates point, Duration time) {
        return ((12 + hourAngle(point, time) / 15) % 24 + 24) % 24;
    }

    // How high the Sun is above the horizon, in degrees; negative at night.
    public double elevation(Coordinates point, Duration time) {
        double latitude = Math.toRadians(point.latitude());
        double declination = Math.toRadians(declination(time));
        double hourAngle = Math.toRadians(hourAngle(point, time));
        double sine = Math.sin(latitude) * Math.sin(declination)
                + Math.cos(latitude) * Math.cos(declination) * Math.cos(hourAngle);
        return Math.toDegrees(Math.asin(Math.clamp(sine, -1, 1)));
    }

    // The power of sunlight on a flat surface on the ground, in W/m2: none at night. The light crosses the
    // atmosphere along a path 1 / sin(elevation) times its thickness (Beer-Lambert law).
    public double irradiance(Coordinates point, Duration time) {
        double sine = Math.sin(Math.toRadians(elevation(point, time)));
        if (sine <= 0) {
            return 0;
        }
        double distance = onOrbit(time).relativeDistance();
        return planet.orbit().solarConstantWattsPerSquareMeter() / (distance * distance) * sine
                * Math.exp(-opticalDepth(time) / sine);
    }

    // How far the point is from noon, in degrees: 0 at noon, positive in the afternoon (15 degrees per hour).
    private double hourAngle(Coordinates point, Duration time) {
        return ((point.longitude() - noonLongitude(time)) % 360 + 540) % 360 - 180;
    }

    private double sols(Duration time) {
        return time.toMillis() / (double) planet.dayLength().toMillis();
    }
}
