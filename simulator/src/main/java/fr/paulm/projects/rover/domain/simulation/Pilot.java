package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Rover;

import java.util.List;
import java.util.Optional;

// Who drives a rover on the common clock: it gives the next instruction, from the state the rover is in, or none
// when the rover has nothing left to do. A list of orders is the simplest pilot; a pilot that discovers the ground
// as the rover goes can change its mind (see AutoNav).
@FunctionalInterface
public interface Pilot {

    Optional<Instruction> next(Rover state);

    // The orders, one after the other.
    static Pilot of(List<Instruction> orders) {
        return new Pilot() {
            private int next;

            @Override
            public Optional<Instruction> next(Rover state) {
                return next < orders.size() ? Optional.of(orders.get(next++)) : Optional.empty();
            }
        };
    }
}
