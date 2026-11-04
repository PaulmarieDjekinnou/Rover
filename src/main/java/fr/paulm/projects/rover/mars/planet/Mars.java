package fr.paulm.projects.rover.mars.planet;

import java.time.Duration;

// The physical constants of Mars that the simulation uses.
public final class Mars {

    // The mean radius: distances are computed on a sphere of this radius
    public static final double MEAN_RADIUS_METERS = 3_389_500;

    // A Martian day: 24 h 39 min 35 s. Durations are shown in sols, as in real missions
    public static final Duration SOL = Duration.ofSeconds(88_775);

    // A year of 668.6 sols, an axis tilted by 25.19 degrees (close to the Earth's 23.44, so Mars has seasons too),
    // and about 586 W/m2 of sunlight above the atmosphere, 43 % of what the Earth gets
    public static final Orbit ORBIT = new Orbit(668.6, 25.19, 586);

    private Mars() {
    }
}
