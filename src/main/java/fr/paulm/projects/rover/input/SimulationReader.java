package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.simulation.SimulationInput;

// A possible input for the simulation (text file, and later JSON, database…).
public interface SimulationReader {

    SimulationInput read();
}
