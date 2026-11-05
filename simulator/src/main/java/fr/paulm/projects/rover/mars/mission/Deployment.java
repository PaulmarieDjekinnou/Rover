package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;

import java.util.List;

// The rovers on the ground, ready to carry out their orders: the planet and its Sun, the touchdown of every rover
// in input order, and the simulation input, which holds only the rovers that landed safely, in the same order.
public record Deployment(Planet planet, Sun sun, List<Touchdown> touchdowns, SimulationInput input) {

    public Deployment {
        touchdowns = List.copyOf(touchdowns);
    }

    // The touchdowns of the rovers that landed safely, in the order of the simulation input.
    public List<Touchdown> landed() {
        return touchdowns.stream().filter(Touchdown::landed).toList();
    }
}
