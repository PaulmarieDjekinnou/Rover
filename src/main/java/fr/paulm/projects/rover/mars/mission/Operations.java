package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;

// How a mission operates its rovers each sol, as real missions do: a rover drives and turns only during a window
// of local solar time (in hours of the planet), and drives at most a given distance per sol (its local sol).
// Outside the window, or once its distance is done, it waits for the next sol.
// By default, from 10:00 to 15:00 and 100 m per sol: real rovers drive a few hours a day, and rarely more than
// a few hundred meters in a sol.
public record Operations(double windowStartHours, double windowEndHours, double maxMetersPerSol) {

    public static final Operations DEFAULT = new Operations(10, 15, 100);

    // No limits: a rover drives at any time, as far as its energy allows.
    public static final Operations UNLIMITED = new Operations(0, 24, Double.POSITIVE_INFINITY);

    public Operations {
        if (!(windowStartHours >= 0 && windowStartHours < windowEndHours && windowEndHours <= 24)
                || !(maxMetersPerSol > 0)) {
            throw new InvalidInputException("Invalid operations: a drive window from " + windowStartHours + " to "
                    + windowEndHours + " hours and " + maxMetersPerSol
                    + " m per sol. Expected: a window within the day, and a positive distance");
        }
    }

    // Whether this local time (in hours, from 0 to 24) is in the drive window.
    public boolean inWindow(double localHours) {
        return localHours >= windowStartHours && localHours < windowEndHours;
    }
}
