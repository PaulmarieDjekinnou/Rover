package fr.paulm.projects.rover.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public final class Simulation {
    private static final Logger LOGGER = LoggerFactory.getLogger(Simulation.class);

    private final SimulationInput simulationInput;

    public Simulation(SimulationInput simulationInput) {
        this.simulationInput = simulationInput;
    }

    // Moves each rover according to its instructions, in input order,
    // and logs its final position as soon as it has finished.
    // All rovers are on the plateau from the start: a rover cannot move onto the cell of a rover
    // that has finished (its final position) or that has not started yet (its starting position).
    public void run() {
        Set<Position> occupiedCells = new HashSet<>();
        simulationInput.missions().forEach(mission -> occupiedCells.add(mission.rover().position()));

        for (RoverMission mission : simulationInput.missions()) {
            Rover start = mission.rover();
            occupiedCells.remove(start.position());
            Rover finalRover = start.execute(mission.instructions(), simulationInput.plateau(), Set.copyOf(occupiedCells));
            occupiedCells.add(finalRover.position());
            LOGGER.info("{}", finalRover);
        }
    }
}
