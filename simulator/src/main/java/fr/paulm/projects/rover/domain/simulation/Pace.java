package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;

// How long a rover takes to carry out an instruction on the common clock (going from one state to the next),
// once the terrain has said how long the rover is busy with it: the rover may have to wait first, for instance for its battery to recharge, or it may
// never manage to, and stop for good where it is, leaving its other instructions undone.
// The clock asks at the start of each instruction, in time order for each rover.
// The pace also says when each rover starts (when it lands, on a planet).
@FunctionalInterface
public interface Pace {

    // Nothing to wait for: an instruction takes the time the terrain gives it, and every rover starts at once.
    Pace IMMEDIATE = (rover, from, to, start, busy) -> Timing.done(busy, Duration.ZERO);

    Timing timing(int rover, Rover from, Rover to, Duration start, Duration busy);

    // When the rover starts its first instruction, on the common clock.
    default Duration start(int rover) {
        return Duration.ZERO;
    }

    // How an instruction went: how long it took from its start, and how much of it the rover spent waiting.
    // A rover that stopped did not carry it out: the duration is the time it took to give up.
    record Timing(Duration duration, Duration waiting, boolean stopped) {

        public static Timing done(Duration busy, Duration waiting) {
            return new Timing(busy.plus(waiting), waiting, false);
        }

        public static Timing stoppedAfter(Duration duration, Duration waiting) {
            return new Timing(duration, waiting, true);
        }
    }
}
