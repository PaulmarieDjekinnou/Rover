package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Dust;
import fr.paulm.projects.rover.mars.planet.DustWeather;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;

// The sky of a mission: the season at the landing (the solar longitude, Ls, in degrees), the local solar time
// of the landing (in hours of the planet, at the site the first rover aims at), and the dust in the atmosphere:
// its optical depth on a clear day and, with weather, the dust season and the storms of DustWeather.
// By default, noon at the northern spring equinox, with the weather of Mars on a clear base of 0.5.
public record Sky(double startSolarLongitudeDegrees, double landingHours, double dustOpacity, boolean weather) {

    public static final Sky DEFAULT = new Sky(0, 12, 0.5, true);

    public Sky {
        if (!(landingHours >= 0 && landingHours < 24)) {
            throw new InvalidInputException("Invalid landing time: " + landingHours + " hours. Expected: from 00:00 to 23:59");
        }
    }

    // A sky whose dust does not change.
    public Sky(double startSolarLongitudeDegrees, double landingHours, double dustOpacity) {
        this(startSolarLongitudeDegrees, landingHours, dustOpacity, false);
    }

    // The Sun of the mission, where the landing time is the local time at the given site; the storms are drawn
    // from the seed.
    public Sun sun(Planet planet, Coordinates site, long seed) {
        double noonLongitude = site.longitude() - 15 * (landingHours - 12);
        Dust dust = weather ? new DustWeather(dustOpacity, planet, seed) : Dust.constant(dustOpacity);
        return new Sun(planet, startSolarLongitudeDegrees, noonLongitude, dust);
    }

    // The Sun under a dust that does not change, for tests.
    public Sun sun(Planet planet, Coordinates site) {
        return new Sky(startSolarLongitudeDegrees, landingHours, dustOpacity).sun(planet, site, 0);
    }
}
