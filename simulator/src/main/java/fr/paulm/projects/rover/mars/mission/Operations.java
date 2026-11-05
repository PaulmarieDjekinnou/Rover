package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;

// How a mission operates its rovers each sol, as real missions do: a rover drives only one sol in a few (the other
// sols go to science, with the rover standing), only during a window of local solar time (in hours of the planet),
// and at most a given distance on a drive sol. Outside of these, it waits.
// By default, one sol in three, from 10:00 to 15:00, 50 m: real rovers average some 9 m per sol over their
// missions (Curiosity 18.9 km in 2039 sols, Opportunity 45.2 km in 5111 sols), and rarely drive more than a few
// hundred meters in a sol (records of about 230 to 350 m).
public record Operations(double windowStartHours, double windowEndHours, double maxMetersPerSol, int driveEverySols) {

    public static final Operations DEFAULT = new Operations(10, 15, 50, 3);

    // No limits: a rover drives at any time, every sol, as far as its energy allows.
    public static final Operations UNLIMITED = new Operations(0, 24, Double.POSITIVE_INFINITY, 1);

    public Operations {
        if (!(windowStartHours >= 0 && windowStartHours < windowEndHours && windowEndHours <= 24)
                || !(maxMetersPerSol > 0) || driveEverySols < 1) {
            throw new InvalidInputException("Invalid operations: a drive window from " + windowStartHours + " to "
                    + windowEndHours + " hours, " + maxMetersPerSol + " m per sol, one sol in " + driveEverySols
                    + ". Expected: a window within the day, a positive distance, and a drive every sol or more");
        }
    }

    // Driving every sol.
    public Operations(double windowStartHours, double windowEndHours, double maxMetersPerSol) {
        this(windowStartHours, windowEndHours, maxMetersPerSol, 1);
    }

    // Whether this local time (in hours, from 0 to 24) is in the drive window.
    public boolean inWindow(double localHours) {
        return localHours >= windowStartHours && localHours < windowEndHours;
    }

    // Whether the rover drives on this local sol (counted from the start of the mission).
    public boolean driveSol(long sol) {
        return Math.floorMod(sol, driveEverySols) == 0;
    }
}
