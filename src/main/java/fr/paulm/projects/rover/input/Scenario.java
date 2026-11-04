package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.mars.mission.MarsMission;

// What an input file describes: rovers on a plateau (the format of the first versions),
// or a mission to a planet, where the rovers must land first.
public sealed interface Scenario {

    record OnPlateau(SimulationInput input) implements Scenario {
    }

    record OnMars(MarsMission mission) implements Scenario {
    }
}
