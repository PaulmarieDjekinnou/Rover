package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Sun;

import java.time.Duration;

// When a rover lands: on a sol of the mission (from 0), at a local solar time at its site, in hours of the planet.
public record LandingDate(int sol, double localHours) {

    public LandingDate {
        if (sol < 0 || !(localHours >= 0 && localHours < 24)) {
            throw new InvalidInputException("Invalid landing date: sol " + sol + " at " + localHours
                    + " hours. Expected: a sol from 0, and a time from 00:00 to 23:59");
        }
    }

    // The time of the mission clock when it is that local time at the site, during that sol.
    public Duration time(Sun sun, Coordinates site, Duration dayLength) {
        Duration solStart = dayLength.multipliedBy(sol);
        double hoursLater = ((localHours - sun.localTime(site, solStart)) % 24 + 24) % 24;
        return solStart.plusMillis(Math.round(hoursLater / 24 * dayLength.toMillis()));
    }
}
