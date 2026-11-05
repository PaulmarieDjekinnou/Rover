package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.rover.PowerSystem;

import java.util.List;
import java.util.Optional;

// What is planned for one rover: its name, where it lands, which way it faces once on the ground, its orders,
// its power, when it lands (when no date is given, it lands at the start of the mission), and the waypoints it
// must reach in order, and its own operations if they differ from the mission's. A rover follows either orders or
// waypoints: the instructions to its waypoints are planned once it has landed (see PlanetMission).
public record RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders, PowerSystem power,
                        Optional<LandingDate> landingDate, List<Coordinates> waypoints, Optional<Operations> operations) {

    public RoverPlan {
        orders = List.copyOf(orders);
        waypoints = List.copyOf(waypoints);
        if (!orders.isEmpty() && !waypoints.isEmpty()) {
            throw new InvalidInputException("Rover \"" + name + "\" has both orders and waypoints. Expected: one or the other");
        }
    }

    // A rover operated as the mission says.
    public RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders, PowerSystem power,
                     Optional<LandingDate> landingDate, List<Coordinates> waypoints) {
        this(name, site, heading, orders, power, landingDate, waypoints, Optional.empty());
    }

    // A rover that follows orders.
    public RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders, PowerSystem power,
                     Optional<LandingDate> landingDate) {
        this(name, site, heading, orders, power, landingDate, List.of());
    }

    // A rover landing at the start of the mission.
    public RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders, PowerSystem power) {
        this(name, site, heading, orders, power, Optional.empty());
    }

    // A rover with the default power system, landing at the start of the mission.
    public RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders) {
        this(name, site, heading, orders, PowerSystem.DEFAULT);
    }
}
