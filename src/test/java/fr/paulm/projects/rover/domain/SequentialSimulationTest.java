package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SequentialSimulationTest {

    private final Simulation simulation = new SequentialSimulation();

    private static List<String> finalRovers(List<MissionResult> results) {
        return results.stream().map(result -> result.finalRover().toString()).toList();
    }

    private static List<Integer> blockedMoves(List<MissionResult> results) {
        return results.stream().map(MissionResult::blockedMoves).toList();
    }

    @Test
    void returnsFinalPositionOfEachRoverInOrder() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(1, 2), Direction.N), TestInstructions.of("LMLMLMLMM")),
                new RoverMission(new Rover(new Position(3, 3), Direction.E), TestInstructions.of("MMRMMRMRRM"))));
        List<MissionResult> results = simulation.run(input);
        assertEquals(List.of("1 3 N", "5 1 E"), finalRovers(results));
        assertEquals(List.of(0, 0), blockedMoves(results));
    }

    @Test
    void roversWrapAroundThePlateau() {
        SimulationInput input = new SimulationInput(new Plateau(2, 2), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MMMM")),
                new RoverMission(new Rover(new Position(1, 0), Direction.E), TestInstructions.of("MMMM"))));
        // On a 3x3 plateau, 4 moves wrap around once
        assertEquals(List.of("0 1 N", "2 0 E"), finalRovers(simulation.run(input)));
    }

    // Collisions

    @Test
    void roverCannotMoveOntoTheFinalPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MMM")),
                new RoverMission(new Rover(new Position(0, 5), Direction.S), TestInstructions.of("MMM"))));
        List<MissionResult> results = simulation.run(input);
        // The first rover ends in (0, 3): the second one stops just above it, its last 2 moves are blocked
        assertEquals(List.of("0 3 N", "0 4 S"), finalRovers(results));
        assertEquals(List.of(0, 2), blockedMoves(results));
    }

    @Test
    void roverCannotMoveOntoTheStartingPositionOfANextRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.E), TestInstructions.of("MMMM")),
                new RoverMission(new Rover(new Position(3, 0), Direction.N), TestInstructions.of("M"))));
        List<MissionResult> results = simulation.run(input);
        // The second rover has not moved yet when the first one runs: it blocks the cell (3, 0)
        assertEquals(List.of("2 0 E", "3 1 N"), finalRovers(results));
        assertEquals(List.of(2, 0), blockedMoves(results));
    }

    @Test
    void roverCanMoveOntoTheStartingPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MM")),
                new RoverMission(new Rover(new Position(1, 0), Direction.W), TestInstructions.of("M"))));
        List<MissionResult> results = simulation.run(input);
        // The first rover has left (0, 0): the cell is free again
        assertEquals(List.of("0 2 N", "0 0 W"), finalRovers(results));
        assertEquals(List.of(0, 0), blockedMoves(results));
    }

    @Test
    void runCanBeReplayedWithTheSameResult() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(1, 2), Direction.N), TestInstructions.of("LMLMLMLMM"))));
        assertEquals(simulation.run(input), simulation.run(input));
    }

    @Test
    void noRoversGivesNoResult() {
        assertEquals(List.of(), simulation.run(new SimulationInput(new Plateau(5, 5), List.of())));
    }

    @Test
    void roverGoesAroundAnotherRover() {
        // Blocked to the north by the second rover in (2, 3), then goes around it by the east
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(2, 2), Direction.N), TestInstructions.of("MRMLMM")),
                new RoverMission(new Rover(new Position(2, 3), Direction.N), TestInstructions.of(""))));
        List<MissionResult> results = simulation.run(input);
        assertEquals(List.of("3 4 N", "2 3 N"), finalRovers(results));
        assertEquals(List.of(1, 0), blockedMoves(results));
    }

    // History: in sequential mode, each instruction executed is a turn

    @Test
    void historyHasOneTurnPerInstruction() {
        SimulationInput input = new SimulationInput(new Plateau(3, 3), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MMM")),
                new RoverMission(new Rover(new Position(0, 3), Direction.S), TestInstructions.of("MMM"))));
        List<Turn> history = simulation.turns(input).toList();
        Rover first = new Rover(new Position(0, 2), Direction.N);
        Rover second = new Rover(new Position(0, 3), Direction.S);
        assertEquals(6, history.size());
        // Turn 1: only the first rover has moved; the second one has not started yet
        assertEquals(new Turn(1, List.of(new Rover(new Position(0, 1), Direction.N), second), Set.of()), history.get(0));
        // Turn 3: the first rover is blocked by the second one; turns 4 to 6: the second one is blocked by the first one
        assertEquals(new Turn(3, List.of(first, second), Set.of(0)), history.get(2));
        assertEquals(new Turn(6, List.of(first, second), Set.of(1)), history.get(5));
    }

    @Test
    void lastTurnOfHistoryMatchesTheResults() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(1, 2), Direction.N), TestInstructions.of("LMLMLMLMM")),
                new RoverMission(new Rover(new Position(3, 3), Direction.E), TestInstructions.of("MMRMMRMRRM"))));
        List<Turn> history = simulation.turns(input).toList();
        List<MissionResult> results = simulation.run(input);
        assertEquals(19, history.size());
        assertEquals(results.stream().map(MissionResult::finalRover).toList(), history.getLast().rovers());
    }

    @Test
    void turnsCanBeReadOnlyInPart() {
        // Each turn is computed from the previous one when it is read: the first turns do not depend on the next ones
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(1, 2), Direction.N), TestInstructions.of("LMLMLMLMM"))));
        assertEquals(simulation.turns(input).toList().subList(0, 3), simulation.turns(input).limit(3).toList());
    }
}
