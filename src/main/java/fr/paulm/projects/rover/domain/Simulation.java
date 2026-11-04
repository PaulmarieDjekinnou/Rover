package fr.paulm.projects.rover.domain;

import java.util.List;
import java.util.stream.Stream;

// A way of running the missions (strategy). Each implementation decides in which order the rovers move;
// the rules are the same for all of them: the plateau wraps around and two rovers never share a cell.
// A simulation only computes: displaying the turns and the results is up to the caller,
// so the domain has no side effect.
public interface Simulation {

    // The turns of the simulation, in order. The stream is lazy: each turn is computed from the previous one
    // only when it is read, and nothing is kept once it has been read. Like any stream, it can be read only once.
    Stream<Turn> turns(SimulationInput input);

    // Runs all the missions and returns how each one ended, in input order.
    default List<MissionResult> run(SimulationInput input) {
        ResultTracker tracker = new ResultTracker(input);
        turns(input).forEach(tracker::record);
        return tracker.results();
    }
}
