package fr.paulm.projects.rover.domain;

// A way of running the missions (strategy). Each implementation decides in which order the rovers move;
// the rules are the same for all of them: the plateau wraps around and two rovers never share a cell.
// A simulation only computes results: displaying them is up to the caller, so the domain has no side effect.
public interface Simulation {

    // Runs all the missions and returns how each one ended, in input order, with the state of the rovers
    // after each turn.
    SimulationResult run(SimulationInput input);
}
