package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;

// The sky of a mission: the season at the landing (the solar longitude, Ls, in degrees), the local solar time
// of the landing (in hours of the planet, at the site the first rover aims at), and the dust in the atmosphere
// (its optical depth). By default, noon at the northern spring equinox, under a clear sky.
public record Sky(double startSolarLongitudeDegrees, double landingHours, double dustOpacity) {

    public static final Sky DEFAULT = new Sky(0, 12, 0.5);

    public Sky {
        if (!(landingHours >= 0 && landingHours < 24)) {
            throw new InvalidInputException("Invalid landing time: " + landingHours + " hours. Expected: from 00:00 to 23:59");
        }
    }

    // The Sun of the mission, where the landing time is the local time at the given site.
    public Sun sun(Planet planet, Coordinates site) {
        double noonLongitude = site.longitude() - 15 * (landingHours - 12);
        return new Sun(planet, startSolarLongitudeDegrees, noonLongitude, dustOpacity);
    }
}
