package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static fr.paulm.projects.rover.domain.simulation.TestMissions.CLIFF_EAST_OF_COLUMN_1;
import static fr.paulm.projects.rover.domain.simulation.TestMissions.mission;
import static fr.paulm.projects.rover.domain.simulation.TestMissions.result;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SequentialSimulationTest {

    private final Simulation simulation = new SequentialSimulation();

    private static List<String> finalRovers(List<MissionResult> results) {
        return results.stream().map(result -> result.finalRover().toString()).toList();
    }

    private static List<Integer> blockedMoves(List<MissionResult> results) {
        return results.stream().map(MissionResult::blockedByRovers).toList();
    }

    @Test
    void returnsFinalPositionOfEachRoverInOrder() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                mission(1, 2, Direction.N, "LMLMLMLMM"),
                mission(3, 3, Direction.E, "MMRMMRMRRM")));
        List<MissionResult> results = simulation.run(input);
        assertEquals(List.of("1 3 N", "5 1 E"), finalRovers(results));
        assertEquals(List.of(0, 0), blockedMoves(results));
    }

    @Test
    void roversWrapAroundThePlateau() {
        SimulationInput input = new SimulationInput(new Plateau(2, 2), List.of(
                mission(0, 0, Direction.N, "MMMM"),
                mission(1, 0, Direction.E, "MMMM")));
        // On a 3x3 plateau, 4 moves wrap around once
        assertEquals(List.of("0 1 N", "2 0 E"), finalRovers(simulation.run(input)));
    }

    // Collisions

    @Test
    void roverCannotMoveOntoTheFinalPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                mission(0, 0, Direction.N, "MMM"),
                mission(0, 5, Direction.S, "MMM")));
        List<MissionResult> results = simulation.run(input);
        // The first rover ends in (0, 3): the second one stops just above it, its last 2 moves are blocked
        assertEquals(List.of("0 3 N", "0 4 S"), finalRovers(results));
        assertEquals(List.of(0, 2), blockedMoves(results));
    }

    @Test
    void roverCannotMoveOntoTheStartingPositionOfANextRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                mission(0, 0, Direction.E, "MMMM"),
                mission(3, 0, Direction.N, "M")));
        List<MissionResult> results = simulation.run(input);
        // The second rover has not moved yet when the first one runs: it blocks the cell (3, 0)
        assertEquals(List.of("2 0 E", "3 1 N"), finalRovers(results));
        assertEquals(List.of(2, 0), blockedMoves(results));
    }

    @Test
    void roverCanMoveOntoTheStartingPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                mission(0, 0, Direction.N, "MM"),
                mission(1, 0, Direction.W, "M")));
        List<MissionResult> results = simulation.run(input);
        // The first rover has left (0, 0): the cell is free again
        assertEquals(List.of("0 2 N", "0 0 W"), finalRovers(results));
        assertEquals(List.of(0, 0), blockedMoves(results));
    }

    @Test
    void runCanBeReplayedWithTheSameResult() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                mission(1, 2, Direction.N, "LMLMLMLMM")));
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
                mission(2, 2, Direction.N, "MRMLMM"),
                mission(2, 3, Direction.N, "")));
        List<MissionResult> results = simulation.run(input);
        assertEquals(List.of("3 4 N", "2 3 N"), finalRovers(results));
        assertEquals(List.of(1, 0), blockedMoves(results));
    }

    // History: in sequential mode, each instruction executed is a turn, sent to the listener

    @Test
    void historyHasOneTurnPerInstruction() {
        SimulationInput input = new SimulationInput(new Plateau(3, 3), List.of(
                mission(0, 0, Direction.N, "MMM"),
                mission(0, 3, Direction.S, "MMM")));
        List<Turn> history = new ArrayList<>();
        simulation.run(input, history::add);
        Rover first = new Rover(new Position(0, 2), Direction.N);
        Rover second = new Rover(new Position(0, 3), Direction.S);
        assertEquals(6, history.size());
        // Turn 1: only the first rover has moved; the second one has not started yet
        assertEquals(new Turn(1, List.of(new Rover(new Position(0, 1), Direction.N), second), Set.of(), Duration.ZERO, OptionalInt.of(0)), history.get(0));
        // Turn 3: the first rover is blocked by the second one; turns 4 to 6: the second one is blocked by the first one
        assertEquals(new Turn(3, List.of(first, second), Set.of(0), Duration.ZERO, OptionalInt.of(0)), history.get(2));
        assertEquals(new Turn(6, List.of(first, second), Set.of(1), Duration.ZERO, OptionalInt.of(1)), history.get(5));
    }

    @Test
    void lastTurnOfHistoryMatchesTheResults() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                mission(1, 2, Direction.N, "LMLMLMLMM"),
                mission(3, 3, Direction.E, "MMRMMRMRRM")));
        List<Turn> history = new ArrayList<>();
        List<MissionResult> results = simulation.run(input, history::add);
        assertEquals(19, history.size());
        assertEquals(results.stream().map(MissionResult::finalRover).toList(), history.getLast().rovers());
    }

    @Test
    void terrainBlocksTheMovesItDoesNotAllow() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), CLIFF_EAST_OF_COLUMN_1, List.of(
                mission(0, 0, Direction.E, "MMM"),
                mission(2, 1, Direction.W, "M")));
        assertEquals(List.of(result(1, 0, Direction.E, 0, 2), result(2, 1, Direction.W, 0, 1)), simulation.run(input));
    }

    @Test
    void eachRoverMeasuresItsOwnDistanceAndTime() {
        // Each move is 10 m long and takes 1 minute; a turn takes 10 seconds. Blocked moves take no time.
        Terrain measured = new Terrain() {
            @Override
            public boolean canDrive(Position from, Position to) {
                return CLIFF_EAST_OF_COLUMN_1.canDrive(from, to);
            }

            @Override
            public double distance(Position from, Position to) {
                return 10;
            }

            @Override
            public Duration driveTime(Position from, Position to) {
                return Duration.ofMinutes(1);
            }

            @Override
            public Duration turnTime() {
                return Duration.ofSeconds(10);
            }
        };
        SimulationInput input = new SimulationInput(new Plateau(5, 5), measured, List.of(
                mission(0, 0, Direction.N, "MRMM"),
                mission(4, 4, Direction.S, "")));
        List<MissionResult> results = simulation.run(input);
        assertEquals(new MissionResult(new Rover(new Position(1, 1), Direction.E), 0, 1, 20, Duration.ofSeconds(130)),
                results.get(0));
        assertEquals(new MissionResult(new Rover(new Position(4, 4), Direction.S), 0, 0, 0, Duration.ZERO), results.get(1));
    }

    @Test
    void roversSharingCellsNeverBlockEachOther() {
        // Two rovers start in the same cell, then the second one drives onto the cell of the first one and back
        SimulationInput input = new SimulationInput(new Plateau(5, 5), Terrain.FLAT, false, List.of(
                mission(0, 0, Direction.E, "M"),
                mission(0, 0, Direction.N, "RMMBB")));
        assertEquals(List.of(result(1, 0, Direction.E, 0, 0), result(0, 0, Direction.E, 0, 0)), simulation.run(input));
    }
}
