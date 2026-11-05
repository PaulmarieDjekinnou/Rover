package fr.paulm.projects.rover.mars.planet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrbitTest {

    private final Orbit circle = new Orbit(668.6, 25.19, 586);

    // The fraction of a year from a season to another one on the orbit of Mars, found by bisection
    private double yearsBetween(double fromLs, double toLs) {
        double span = ((toLs - fromLs) % 360 + 360) % 360;
        double low = 0;
        double high = 1;
        for (int i = 0; i < 60; i++) {
            double middle = (low + high) / 2;
            double reached = ((Mars.ORBIT.after(fromLs, middle).solarLongitudeDegrees() - fromLs) % 360 + 360) % 360;
            if (reached < span) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    @Test
    void onACircleTheSeasonsGoAtASteadyPace() {
        assertEquals(90, circle.after(0, 0.25).solarLongitudeDegrees(), 1e-9);
        assertEquals(1, circle.after(0, 0.25).relativeDistance(), 1e-12);
    }

    @Test
    void theSeasonsOfMarsDoNotLastTheSameTime() {
        // The northern spring (Ls 0 to 90) is the longest season, the northern autumn (Ls 180 to 270) the shortest
        assertEquals(194, yearsBetween(0, 90) * 668.6, 3);
        assertEquals(142, yearsBetween(180, 270) * 668.6, 3);
    }

    @Test
    void sunlightIsStrongestAtThePerihelion() {
        Orbit.Position perihelion = Mars.ORBIT.after(251, 0);
        Orbit.Position aphelion = Mars.ORBIT.after(71, 0);
        assertEquals(1 - 0.0934, perihelion.relativeDistance(), 1e-6);
        assertEquals(1 + 0.0934, aphelion.relativeDistance(), 1e-6);
        // About 713 and 490 W/m2 above the atmosphere
        assertEquals(713, 586 / Math.pow(perihelion.relativeDistance(), 2), 1);
        assertEquals(490, 586 / Math.pow(aphelion.relativeDistance(), 2), 2);
    }

    @Test
    void aYearLaterTheSeasonIsBack() {
        assertEquals(150, Mars.ORBIT.after(150, 1).solarLongitudeDegrees(), 1e-6);
    }
}
