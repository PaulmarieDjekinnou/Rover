package fr.paulm.projects.rover.domain.simulation;

import java.util.List;

// A way of running the missions (strategy). Each implementation decides in which order the rovers move;
// the rules are the same for all of them: the plateau wraps around and two rovers never share a cell.
// A simulation only computes results: displaying them is up to the caller, which receives the results
// and, through a TurnListener, each turn. The domain itself has no side effect.
public interface Simulation {

    // Runs all the missions and returns how each one ended, in input order.
    // The listener receives the state of the rovers after each turn, as soon as the turn is over.
    List<MissionResult> run(SimulationInput input, TurnListener listener);

    // Runs the simulation for its results only, without following the turns.
    default List<MissionResult> run(SimulationInput input) {
        return run(input, TurnListener.NONE);
    }
}
