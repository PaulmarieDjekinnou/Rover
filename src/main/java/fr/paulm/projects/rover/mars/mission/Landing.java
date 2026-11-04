package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.rover.MarsTerrain;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.util.Arrays;
import java.util.Locale;
import java.util.random.RandomGenerator;

// Sending a rover down to the surface: checking the site it aims at, then drawing where it really touches down.
public final class Landing {
    private final Planet planet;
    private final MarsTerrain terrain;
    private final LandingRules rules;

    public Landing(Planet planet, MarsTerrain terrain, LandingRules rules) {
        this.planet = planet;
        this.terrain = terrain;
        this.rules = rules;
    }

    // Refuses a site that breaks the landing rules, or where the ground around the target cell is too steep
    // for the rover to drive away: landing on a slope could tip it over.
    public void check(Coordinates target) {
        Position cell = planet.cellAt(target);
        int elevation = planet.elevation(cell);
        if (elevation > rules.maxElevationMeters()) {
            throw refused(target, String.format(Locale.ROOT, "too high (%d m). Expected: at most %.0f m",
                    elevation, rules.maxElevationMeters()));
        }
        if (Math.abs(target.latitude()) > rules.maxLatitudeDegrees()) {
            throw refused(target, String.format(Locale.ROOT, "too far from the equator. Expected: a latitude within %.1f degrees",
                    rules.maxLatitudeDegrees()));
        }
        boolean steep = Arrays.stream(Direction.values())
                .anyMatch(direction -> !terrain.canDrive(cell, planet.grid().next(cell, direction).position()));
        if (steep) {
            throw refused(target, "too steep: the rover could not drive away in every direction");
        }
    }

    private InvalidInputException refused(Coordinates target, String reason) {
        return new InvalidInputException("Landing site " + target + " is " + reason);
    }

    // The point where the rover touches down: drawn uniformly in the ellipse around the target.
    // The square root spreads the points evenly over the area (without it, they would gather near the center).
    // Always draws two numbers, so that one rover's ellipse does not change where the next rovers land.
    public Coordinates touchdown(LandingSite site, RandomGenerator random) {
        double distance = Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * 2 * Math.PI;
        Ellipse ellipse = site.ellipse();
        double along = distance * Math.cos(angle) * ellipse.majorAxisKm() * 1_000 / 2;
        double across = distance * Math.sin(angle) * ellipse.minorAxisKm() * 1_000 / 2;
        double azimuth = Math.toRadians(ellipse.azimuthDegrees());
        double north = along * Math.cos(azimuth) - across * Math.sin(azimuth);
        double east = along * Math.sin(azimuth) + across * Math.cos(azimuth);
        return site.target().shiftedBy(north, east, planet.radiusMeters());
    }

    // Whether a rover in this cell can drive to at least one of its neighbours.
    public boolean canLeave(Position cell) {
        return Arrays.stream(Direction.values())
                .anyMatch(direction -> terrain.canDrive(cell, planet.grid().next(cell, direction).position()));
    }
}
