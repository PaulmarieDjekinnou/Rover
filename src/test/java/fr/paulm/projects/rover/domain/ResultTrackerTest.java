package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultTrackerTest {

    private final Rover first = new Rover(new Position(0, 0), Direction.N);
    private final Rover second = new Rover(new Position(2, 2), Direction.E);
    private final SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
            new RoverMission(first, TestInstructions.of("MM")),
            new RoverMission(second, TestInstructions.of("M"))));

    @Test
    void withoutTurnsTheRoversStayOnTheirStartingCell() {
        assertEquals(List.of(new MissionResult(first, 0), new MissionResult(second, 0)), new ResultTracker(input).results());
    }

    @Test
    void lastTurnGivesTheFinalRoversAndBlockedMovesAreCounted() {
        Rover firstMoved = new Rover(new Position(0, 1), Direction.N);
        ResultTracker tracker = new ResultTracker(input);
        tracker.record(new Turn(1, List.of(firstMoved, second), Set.of(1)));
        tracker.record(new Turn(2, List.of(firstMoved, second), Set.of(0, 1)));
        assertEquals(List.of(new MissionResult(firstMoved, 1), new MissionResult(second, 2)), tracker.results());
    }
}
