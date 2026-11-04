package fr.paulm.projects.rover.mars.planet;

import java.time.Duration;

// The physical constants of Mars that the simulation uses.
public final class Mars {

    // The mean radius: distances are computed on a sphere of this radius
    public static final double MEAN_RADIUS_METERS = 3_389_500;

    // A Martian day: 24 h 39 min 35 s. Durations are shown in sols, as in real missions
    public static final Duration SOL = Duration.ofSeconds(88_775);

    private Mars() {
    }
}
