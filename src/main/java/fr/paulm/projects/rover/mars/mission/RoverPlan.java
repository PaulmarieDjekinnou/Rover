package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;

import java.util.List;

// What is planned for one rover: its name, where it lands, which way it faces once on the ground, and its orders.
public record RoverPlan(String name, LandingSite site, Direction heading, List<Instruction> orders) {

    public RoverPlan {
        orders = List.copyOf(orders);
    }
}
