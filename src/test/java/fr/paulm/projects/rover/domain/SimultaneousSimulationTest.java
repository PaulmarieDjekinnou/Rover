package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimultaneousSimulationTest {

    private final Simulation simulation = new SimultaneousSimulation();

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
    void roversWantingTheSameCellAreAllBlocked() {
        // No priority: neither the first nor the second rover gets (1, 0)
        assertEquals(List.of(result(0, 0, Direction.E, 1), result(2, 0, Direction.W, 1)),
                run(mission(0, 0, Direction.E, "M"), mission(2, 0, Direction.W, "M")));
    }

    @Test
    void roversCannotSwapTheirCells() {
        assertEquals(List.of(result(0, 0, Direction.E, 1), result(1, 0, Direction.W, 1)),
                run(mission(0, 0, Direction.E, "M"), mission(1, 0, Direction.W, "M")));
    }

    @Test
    void roverCanFollowARoverThatLeavesItsCell() {
        // The first rover moves onto (1, 0) while the second one leaves it.
        // In step-by-step mode, the first rover would be blocked, because the second one has not moved yet.
        assertEquals(List.of(result(1, 0, Direction.E, 0), result(2, 0, Direction.E, 0)),
                run(mission(0, 0, Direction.E, "M"), mission(1, 0, Direction.E, "M")));
    }

    @Test
    void blockedRoverBlocksTheRoverBehindIt() {
        // The third rover does not move: it blocks the second one, which then blocks the first one
        assertEquals(List.of(result(0, 0, Direction.E, 1), result(1, 0, Direction.E, 1), result(2, 0, Direction.N, 0)),
                run(mission(0, 0, Direction.E, "M"), mission(1, 0, Direction.E, "M"), mission(2, 0, Direction.N, "")));
    }

    @Test
    void roversCanTurnAroundASquareTogether() {
        // Four rovers each take the cell of the next one: nobody is blocked
        assertEquals(List.of(
                        result(0, 1, Direction.N, 0), result(1, 1, Direction.E, 0),
                        result(1, 0, Direction.S, 0), result(0, 0, Direction.W, 0)),
                run(mission(0, 0, Direction.N, "M"), mission(0, 1, Direction.E, "M"),
                        mission(1, 1, Direction.S, "M"), mission(1, 0, Direction.W, "M")));
    }

    @Test
    void roverThatHasFinishedStaysOnItsCell() {
        // Turn 1: both rovers want (0, 1) and are blocked. Turn 2: the first rover has finished, the second one moves
        assertEquals(List.of(result(0, 0, Direction.N, 1), result(0, 1, Direction.S, 1)),
                run(mission(0, 0, Direction.N, "M"), mission(0, 2, Direction.S, "MM")));
    }

    @Test
    void turningIsNeverBlocked() {
        assertEquals(List.of(result(0, 0, Direction.E, 0), result(1, 0, Direction.N, 0)),
                run(mission(0, 0, Direction.N, "R"), mission(1, 0, Direction.E, "L")));
    }

    @Test
    void noRoversGivesNoResult() {
        assertEquals(List.of(), run());
    }

    // History: each turn, every rover that still has instructions executes one

    @Test
    void historyShowsEachTurn() {
        // Turn 1: both rovers move together; turn 2: the second rover has finished and blocks the first one
        List<Turn> history = simulation.run(new SimulationInput(new Plateau(5, 5), List.of(
                mission(0, 0, Direction.E, "MM"), mission(1, 0, Direction.E, "M")))).history();
        Rover second = new Rover(new Position(2, 0), Direction.E);
        assertEquals(List.of(
                new Turn(1, List.of(new Rover(new Position(1, 0), Direction.E), second), Set.of()),
                new Turn(2, List.of(new Rover(new Position(1, 0), Direction.E), second), Set.of(0))),
                history);
    }
}
