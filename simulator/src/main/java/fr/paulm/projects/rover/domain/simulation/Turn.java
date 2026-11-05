package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Rover;

import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

// The state of all the rovers after one turn (numbered from 1), in input order.
// blockedRovers holds the index (from 0, in input order) of each rover whose move was blocked in this turn.
// What a turn is depends on the simulation mode: one instruction of one rover in sequential mode,
// one instruction of every rover in the other modes, and one instruction of one rover, when it ends,
// on a common clock (ClockSimulation). time is the time of the common clock at the end of the turn;
// the modes without a clock leave it at zero. actor is the rover (its index) whose instruction made the turn,
// when a turn is one instruction of one rover (sequential mode and common clock); empty otherwise.
public record Turn(int number, List<Rover> rovers, Set<Integer> blockedRovers, Duration time, OptionalInt actor) {

    // Copies are cheap: rovers are immutable, so a snapshot only copies references.
    public Turn {
        rovers = List.copyOf(rovers);
        blockedRovers = Set.copyOf(blockedRovers);
    }

    // A turn of every rover, with no clock.
    public Turn(int number, List<Rover> rovers, Set<Integer> blockedRovers) {
        this(number, rovers, blockedRovers, Duration.ZERO, OptionalInt.empty());
    }
}
