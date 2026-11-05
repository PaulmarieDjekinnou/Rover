package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.RoverMission;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;
import fr.paulm.projects.rover.mars.rover.Mobility;
import fr.paulm.projects.rover.mars.rover.ReliefTerrain;
import fr.paulm.projects.rover.mars.rover.RoutePlanner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

// A mission to a planet: the planet, how the rovers drive, the landing rules, the sky (season and dust),
// how the rovers are operated each sol, the seed of the random draws, and the plan of each rover. The same seed always gives the same landings,
// so a mission can be replayed.
public record PlanetMission(Planet planet, Mobility mobility, LandingRules landingRules, Sky sky, Operations operations,
                            long seed, List<RoverPlan> rovers) {

    // A mission with the default operations.
    public PlanetMission(Planet planet, Mobility mobility, LandingRules landingRules, Sky sky, long seed,
                         List<RoverPlan> rovers) {
        this(planet, mobility, landingRules, sky, Operations.DEFAULT, seed, rovers);
    }

    // A mission under the default sky, with the default operations.
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

    // Lands the rovers one after the other, in input order, each at its own date (see RoverPlan). A rover that touches down too close to a rover already
    // there, or in a cell it cannot leave, is lost: it takes no part in the simulation. Rovers can share a cell:
    // a cell is several kilometers wide.
    public Deployment land() {
        SplittableRandom random = new SplittableRandom(seed);
        Landing landing = landing();
        List<Coordinates> landedPoints = new ArrayList<>();
        List<Touchdown> touchdowns = new ArrayList<>();
        List<RoverMission> missions = new ArrayList<>();
        Sun sun = sun();
        for (RoverPlan plan : rovers) {
            Coordinates point = landing.touchdown(plan.site(), random);
            Position cell = planet.cellAt(point);
            Optional<LandingFailure> failure = failureAt(point, cell, landedPoints, landing);
            Duration time = plan.landingDate()
                    .map(date -> date.time(sun, plan.site().target(), planet.dayLength()))
                    .orElse(Duration.ZERO);
            Route route = failure.isEmpty() ? route(plan, new Rover(cell, plan.heading())) : Route.of(plan.orders());
            touchdowns.add(new Touchdown(plan, time, point, cell, failure, route));
            if (failure.isEmpty()) {
                landedPoints.add(point);
                missions.add(new RoverMission(new Rover(cell, plan.heading()), route.instructions()));
            }
        }
        return new Deployment(planet, sun, touchdowns,
                new SimulationInput(planet.grid(), terrain(), false, missions));
    }

    // The orders of the rover, or the route to its waypoints, planned from where it landed, one waypoint after the
    // other, up to the first one it cannot reach.
    private Route route(RoverPlan plan, Rover landed) {
        if (plan.waypoints().isEmpty()) {
            return Route.of(plan.orders());
        }
        RoutePlanner planner = new RoutePlanner(planet, mobility);
        List<Instruction> instructions = new ArrayList<>();
        Rover state = landed;
        int reached = 0;
        for (Coordinates waypoint : plan.waypoints()) {
            Optional<List<Instruction>> leg = planner.route(state, waypoint);
            if (leg.isEmpty()) {
                break;
            }
            for (Instruction instruction : leg.get()) {
                state = state.apply(instruction, planet.grid());
            }
            instructions.addAll(leg.get());
            reached++;
        }
        return new Route(instructions, reached, plan.waypoints().size());
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
