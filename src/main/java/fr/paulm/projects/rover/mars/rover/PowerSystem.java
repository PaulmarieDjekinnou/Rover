package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;

// The power of a rover: its source, and its battery, which starts full.
// By default, a generator like Curiosity's and a battery of 2 kWh.
public record PowerSystem(PowerSource source, double batteryWattHours) {

    public static final PowerSystem DEFAULT = new PowerSystem(new PowerSource.Generator(110), 2_000);

    // What a rover uses: when it drives or turns, and when it waits (heaters, computer, radio)
    public static final double ACTIVE_WATTS = 150;
    public static final double IDLE_WATTS = 15;

    public PowerSystem {
        if (!(batteryWattHours > 0)) {
            throw new InvalidInputException("Invalid battery: " + batteryWattHours + " Wh. Expected: a positive capacity");
        }
    }
}
