package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;

// The landing ellipse: where a rover may touch down around the point it aims at. Real missions give its
// two axes in km (about 20 x 7 km for Curiosity, 7.7 x 6.6 km for Perseverance) and the azimuth of the major
// axis, in degrees clockwise from north.
public record Ellipse(double majorAxisKm, double minorAxisKm, double azimuthDegrees) {

    // A perfect landing, exactly on the target
    public static final Ellipse NONE = new Ellipse(0, 0, 0);

    public Ellipse {
        if (!(minorAxisKm >= 0 && majorAxisKm >= minorAxisKm) || !Double.isFinite(majorAxisKm)
                || !Double.isFinite(azimuthDegrees)) {
            throw new InvalidInputException("Invalid landing ellipse: " + majorAxisKm + " x " + minorAxisKm
                    + " km. Expected: a major axis at least as long as the minor axis, which is not negative");
        }
    }
}
