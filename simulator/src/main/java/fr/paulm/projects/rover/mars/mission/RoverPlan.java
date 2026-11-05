package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.mars.rover.PowerSystem;

import java.util.List;
import java.util.Optional;

// What is planned for one rover: its name, where it lands, which way it faces once on the ground, its orders,
// its power, and when it lands (when no date is given, it lands at the start of the mission).
public record RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders, PowerSystem power,
                        Optional<LandingDate> landingDate) {

    public RoverPlan {
        orders = List.copyOf(orders);
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
