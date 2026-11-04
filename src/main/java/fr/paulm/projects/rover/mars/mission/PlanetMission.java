package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.RoverMission;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.rover.ReliefTerrain;
import fr.paulm.projects.rover.mars.rover.Mobility;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

// A mission to a planet: the planet, how the rovers drive, the landing rules, the sky (season and dust),
// the seed of the random draws, and the plan of each rover. The same seed always gives the same landings,
// so a mission can be replayed.
public record PlanetMission(Planet planet, Mobility mobility, LandingRules landingRules, Sky sky, long seed,
                            List<RoverPlan> rovers) {

    // A mission under the default sky.
    public PlanetMission(Planet planet, Mobility mobility, LandingRules landingRules, long seed, List<RoverPlan> rovers) {
        this(planet, mobility, landingRules, Sky.DEFAULT, seed, rovers);
    }

    // Every rover has its own name, and aims at a site that the landing rules accept.
    public PlanetMission {
        rovers = List.copyOf(rovers);
        Landing landing = new Landing(planet, new ReliefTerrain(planet, mobility), landingRules);
        Set<String> names = new HashSet<>();
        for (RoverPlan rover : rovers) {
            if (!names.add(rover.name())) {
                throw new InvalidInputException("Two rovers are named \"" + rover.name() + "\"");
            }
            landing.check(rover.site().target());
        }
    }

    public ReliefTerrain terrain() {
        return new ReliefTerrain(planet, mobility);
    }

    // The Sun of the mission: the landing time is the local time at the site of the first rover.
    public Sun sun() {
        Coordinates site = rovers.isEmpty() ? new Coordinates(0, 0) : rovers.getFirst().site().target();
        return sky.sun(planet, site);
    }

    public Landing landing() {
        return new Landing(planet, terrain(), landingRules);
    }

    // Lands the rovers one after the other, in input order. A rover that touches down too close to a rover already
    // there, or in a cell it cannot leave, is lost: it takes no part in the simulation. Rovers can share a cell:
    // a cell is several kilometers wide.
    public Deployment land() {
        SplittableRandom random = new SplittableRandom(seed);
        Landing landing = landing();
        List<Coordinates> landedPoints = new ArrayList<>();
        List<Touchdown> touchdowns = new ArrayList<>();
        List<RoverMission> missions = new ArrayList<>();
        for (RoverPlan plan : rovers) {
            Coordinates point = landing.touchdown(plan.site(), random);
            Position cell = planet.cellAt(point);
            Optional<LandingFailure> failure = failureAt(point, cell, landedPoints, landing);
            touchdowns.add(new Touchdown(plan, point, cell, failure));
            if (failure.isEmpty()) {
                landedPoints.add(point);
                missions.add(new RoverMission(new Rover(cell, plan.heading()), plan.orders()));
            }
        }
        return new Deployment(planet, sun(), touchdowns,
                new SimulationInput(planet.grid(), terrain(), false, missions));
    }

    private Optional<LandingFailure> failureAt(Coordinates point, Position cell, List<Coordinates> landedPoints,
                                               Landing landing) {
        boolean tooClose = landedPoints.stream()
                .anyMatch(other -> point.distanceTo(other, planet.radiusMeters()) < Landing.CLEARANCE_METERS);
        if (tooClose) {
            return Optional.of(LandingFailure.ON_ANOTHER_ROVER);
        }
        return landing.canLeave(cell) ? Optional.empty() : Optional.of(LandingFailure.NO_WAY_OUT);
    }
}
