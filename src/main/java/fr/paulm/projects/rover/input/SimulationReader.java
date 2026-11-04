package fr.paulm.projects.rover.input;

// A possible input for the simulation (text file, and later JSON, database…).
public interface SimulationReader {

    Scenario read();
}
