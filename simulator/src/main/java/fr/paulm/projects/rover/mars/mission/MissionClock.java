package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.mars.planet.Planet;

import java.time.Duration;
import java.util.Locale;

// The time of the mission clock as missions give it: the sol (from 0), then the time of day in hours and minutes
// of the planet, a sol being cut into 24 hours, like the local time on Mars. "Sol 2 05:12".
public final class MissionClock {

    private MissionClock() {
    }

    public static String format(Duration time, Planet planet) {
        long day = planet.dayLength().toMillis();
        long sol = time.toMillis() / day;
        long minutes = (time.toMillis() % day) * 24 * 60 / day;
        return String.format(Locale.ROOT, "Sol %d %02d:%02d", sol, minutes / 60, minutes % 60);
    }
}
