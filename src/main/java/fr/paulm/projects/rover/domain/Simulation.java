package fr.paulm.projects.rover.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Simulation {
    private static final Logger LOGGER = LoggerFactory.getLogger(Simulation.class);

    private final SimulationInput simulationInput;

    public Simulation(SimulationInput simulationInput) {
        this.simulationInput = simulationInput;
    }

    // Moves each rover according to its instructions, in input order,
    // and logs its final position as soon as it has finished.
    public void run() {
        for (RoverMission mission : simulationInput.missions()) {
            Rover finalRover = mission.rover().execute(mission.instructions(), simulationInput.plateau());
            LOGGER.info("{}", finalRover);
        }
    }
}
