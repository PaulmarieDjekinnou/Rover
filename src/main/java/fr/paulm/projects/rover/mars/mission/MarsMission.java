package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.RoverMission;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.MarsTerrain;
import fr.paulm.projects.rover.mars.planet.Mobility;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

// A mission to a planet: the planet, how the rovers drive, the landing rules, the seed of the random draws,
// and the plan of each rover. The same seed always gives the same landings, so a mission can be replayed.
public record MarsMission(Planet planet, Mobility mobility, LandingRules landingRules, long seed, List<RoverPlan> rovers) {

    public MarsMission {
        rovers = List.copyOf(rovers);
    }

    public MarsTerrain terrain() {
        return new MarsTerrain(planet, mobility);
    }

    public Landing landing() {
        return new Landing(planet, terrain(), landingRules);
    }

    // Lands the rovers one after the other, in input order. A rover that touches down on a rover already there,
    // or in a cell it cannot leave, is lost: it takes no part in the simulation.
    public Deployment land() {
        SplittableRandom random = new SplittableRandom(seed);
        Landing landing = landing();
        Set<Position> occupied = new HashSet<>();
        List<Touchdown> touchdowns = new ArrayList<>();
        List<RoverMission> missions = new ArrayList<>();
        for (RoverPlan plan : rovers) {
            Coordinates point = landing.touchdown(plan.site(), random);
            Position cell = planet.cellAt(point);
            Optional<LandingFailure> failure = failureAt(cell, occupied, landing);
            touchdowns.add(new Touchdown(plan, point, cell, failure));
            if (failure.isEmpty()) {
                occupied.add(cell);
                missions.add(new RoverMission(new Rover(cell, plan.heading()), plan.orders()));
            }
        }
        return new Deployment(planet, touchdowns, new SimulationInput(planet.grid(), terrain(), missions));
    }

    private Optional<LandingFailure> failureAt(Position cell, Set<Position> occupied, Landing landing) {
        if (occupied.contains(cell)) {
            return Optional.of(LandingFailure.ON_ANOTHER_ROVER);
        }
        return landing.canLeave(cell) ? Optional.empty() : Optional.of(LandingFailure.NO_WAY_OUT);
    }
}
