package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StepByStepSimulationTest {

    private final Simulation simulation = new StepByStepSimulation();

    private static RoverMission mission(int x, int y, Direction direction, String instructions) {
        return new RoverMission(new Rover(new Position(x, y), direction), TestInstructions.of(instructions));
    }

    private List<MissionResult> run(RoverMission... missions) {
        return simulation.run(new SimulationInput(new Plateau(5, 5), List.of(missions))).missions();
    }

    private static MissionResult result(int x, int y, Direction direction, int blockedMoves) {
        return new MissionResult(new Rover(new Position(x, y), direction), blockedMoves);
    }

    @Test
    void roversThatNeverMeetEndAsInSequentialMode() {
        assertEquals(List.of(result(1, 3, Direction.N, 0), result(5, 1, Direction.E, 0)),
                run(mission(1, 2, Direction.N, "LMLMLMLMM"), mission(3, 3, Direction.E, "MMRMMRMRRM")));
    }

    @Test
    void roversMoveInTurns() {
        // Turn 1: the first rover moves to (0, 1) before the second one tries to go there.
        // In sequential mode, the first rover would already be in (0, 3) and the second one would move.
        assertEquals(List.of(result(0, 3, Direction.N, 0), result(1, 1, Direction.W, 1)),
                run(mission(0, 0, Direction.N, "MMM"), mission(1, 1, Direction.W, "M")));
    }

    @Test
    void roverThatHasFinishedStaysOnItsCell() {
        // The first rover ends in (0, 1) after turn 1; the second one comes down to (0, 2), then is blocked twice
        assertEquals(List.of(result(0, 1, Direction.N, 0), result(0, 2, Direction.S, 2)),
                run(mission(0, 0, Direction.N, "M"), mission(0, 3, Direction.S, "MMM")));
    }

    @Test
    void roverCanMoveOntoACellFreedEarlierInTheSameTurn() {
        // The first rover leaves (1, 0), then the second one moves there in the same turn
        assertEquals(List.of(result(1, 1, Direction.N, 0), result(1, 0, Direction.E, 0)),
                run(mission(1, 0, Direction.N, "M"), mission(0, 0, Direction.E, "M")));
    }

    @Test
    void roverCannotMoveOntoTheCellOfARoverThatHasNotMovedYetInThisTurn() {
        // Same rovers in the other order: the second rover is still in (1, 0) when the first one tries to go there
        assertEquals(List.of(result(0, 0, Direction.E, 1), result(1, 1, Direction.N, 0)),
                run(mission(0, 0, Direction.E, "M"), mission(1, 0, Direction.N, "M")));
    }

    @Test
    void roverIsNotBlockedByItsOwnCell() {
        assertEquals(List.of(result(1, 0, Direction.E, 0)), run(mission(0, 0, Direction.N, "RM")));
    }

    @Test
    void noRoversGivesNoResult() {
        assertEquals(List.of(), run());
    }

    // History: each turn, every rover that still has instructions executes one

    @Test
    void historyHasOneTurnPerRoundOfInstructions() {
        List<Turn> history = simulation.run(new SimulationInput(new Plateau(5, 5), List.of(
                mission(0, 0, Direction.N, "MMM"), mission(1, 1, Direction.W, "M")))).history();
        Rover second = new Rover(new Position(1, 1), Direction.W);
        assertEquals(List.of(
                new Turn(1, List.of(new Rover(new Position(0, 1), Direction.N), second), Set.of(1)),
                new Turn(2, List.of(new Rover(new Position(0, 2), Direction.N), second), Set.of()),
                new Turn(3, List.of(new Rover(new Position(0, 3), Direction.N), second), Set.of())),
                history);
    }
}
