package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.mars.rover.PowerSystem;

import java.util.List;

// What is planned for one rover: its name, where it lands, which way it faces once on the ground, its orders,
// and its power.
public record RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders, PowerSystem power) {

    public RoverPlan {
        orders = List.copyOf(orders);
    }

    // A rover with the default power system.
    public RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders) {
        this(name, site, heading, orders, PowerSystem.DEFAULT);
    }
}
