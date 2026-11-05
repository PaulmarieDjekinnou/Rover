package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;

// The power of a rover: its source, and its battery, which starts full.
// By default, a generator like Curiosity's and a battery of 2 kWh.
public record PowerSystem(PowerSource source, double batteryWattHours) {

    public static final PowerSystem DEFAULT = new PowerSystem(new PowerSource.Generator(110), 2_000);

    // What a rover uses: when it drives on flat ground or turns, and when it waits (computer, radio)
    public static final double ACTIVE_WATTS = 150;
    public static final double IDLE_WATTS = 15;

    // The survival heaters of a solar rover at night: they keep its electronics and its batteries warm, more in the
    // cold of winter or at high latitude (a low Sun at noon), from 10 W to 40 W. A rover with a radioisotope generator
    // needs none: the heat of its generator does it, as on Curiosity and Perseverance.
    public static double heaterWatts(double noonElevationDegrees) {
        double warmth = Math.sin(Math.toRadians(Math.clamp(noonElevationDegrees, 0, 90)));
        return 10 + 30 * (1 - warmth);
    }

    // How much more power a drive takes on this slope (in degrees, positive uphill): climbing takes more, up to
    // twice as much at the steepest slope the rover can take; going down takes no more than flat ground.
    public static double slopeFactor(double slopeDegrees, double maxSlopeDegrees) {
        return slopeDegrees <= 0 || maxSlopeDegrees <= 0 ? 1 : 1 + Math.min(slopeDegrees / maxSlopeDegrees, 1);
    }

    public PowerSystem {
        if (!(batteryWattHours > 0)) {
            throw new InvalidInputException("Invalid battery: " + batteryWattHours + " Wh. Expected: a positive capacity");
        }
    }
}
