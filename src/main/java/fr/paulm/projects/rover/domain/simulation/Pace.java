package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;
import java.util.Optional;

// How long a rover takes to carry out an instruction on the common clock, once the terrain has said how long the
// rover is busy with it: the rover may have to wait first, for instance for its battery to recharge.
// The clock asks at the start of each instruction, in time order for each rover. Empty when the rover can no
// longer act: it then stops where it is, and leaves its other instructions undone.
// The pace also says when each rover starts (when it lands, on a planet).
@FunctionalInterface
public interface Pace {

    // Nothing to wait for: an instruction takes the time the terrain gives it, and every rover starts at once.
    Pace IMMEDIATE = (rover, state, start, busy) -> Optional.of(busy);

    Optional<Duration> duration(int rover, Rover state, Duration start, Duration busy);

    // When the rover starts its first instruction, on the common clock.
    default Duration start(int rover) {
        return Duration.ZERO;
    }
}
