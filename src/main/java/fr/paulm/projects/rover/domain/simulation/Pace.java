package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;
import java.util.Optional;

// How long a rover takes to carry out an instruction on the common clock, once the terrain has said how long the
// rover is busy with it: the rover may have to wait first, for instance for its battery to recharge.
// The clock asks at the start of each instruction, in time order for each rover. Empty when the rover can no
// longer act: it then stops where it is, and leaves its other instructions undone.
@FunctionalInterface
public interface Pace {

    // Nothing to wait for: an instruction takes the time the terrain gives it.
    Pace IMMEDIATE = (rover, state, start, busy) -> Optional.of(busy);

    Optional<Duration> duration(int rover, Rover state, Duration start, Duration busy);
}
