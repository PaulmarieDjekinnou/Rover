package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationTest {

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
        List<MissionResult> results = new Simulation(input).run();
        assertEquals(List.of("1 3 N", "5 1 E"), finalRovers(results));
        assertEquals(List.of(0, 0), blockedMoves(results));
    }

    @Test
    void roversWrapAroundThePlateau() {
        SimulationInput input = new SimulationInput(new Plateau(2, 2), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MMMM")),
                new RoverMission(new Rover(new Position(1, 0), Direction.E), TestInstructions.of("MMMM"))));
        // On a 3x3 plateau, 4 moves wrap around once
        assertEquals(List.of("0 1 N", "2 0 E"), finalRovers(new Simulation(input).run()));
    }

    // Collisions

    @Test
    void roverCannotMoveOntoTheFinalPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MMM")),
                new RoverMission(new Rover(new Position(0, 5), Direction.S), TestInstructions.of("MMM"))));
        List<MissionResult> results = new Simulation(input).run();
        // The first rover ends in (0, 3): the second one stops just above it, its last 2 moves are blocked
        assertEquals(List.of("0 3 N", "0 4 S"), finalRovers(results));
        assertEquals(List.of(0, 2), blockedMoves(results));
    }

    @Test
    void roverCannotMoveOntoTheStartingPositionOfANextRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.E), TestInstructions.of("MMMM")),
                new RoverMission(new Rover(new Position(3, 0), Direction.N), TestInstructions.of("M"))));
        List<MissionResult> results = new Simulation(input).run();
        // The second rover has not moved yet when the first one runs: it blocks the cell (3, 0)
        assertEquals(List.of("2 0 E", "3 1 N"), finalRovers(results));
        assertEquals(List.of(2, 0), blockedMoves(results));
    }

    @Test
    void roverCanMoveOntoTheStartingPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MM")),
                new RoverMission(new Rover(new Position(1, 0), Direction.W), TestInstructions.of("M"))));
        List<MissionResult> results = new Simulation(input).run();
        // The first rover has left (0, 0): the cell is free again
        assertEquals(List.of("0 2 N", "0 0 W"), finalRovers(results));
        assertEquals(List.of(0, 0), blockedMoves(results));
    }

    @Test
    void runCanBeReplayedWithTheSameResult() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(1, 2), Direction.N), TestInstructions.of("LMLMLMLMM"))));
        Simulation simulation = new Simulation(input);
        assertEquals(simulation.run(), simulation.run());
    }

    @Test
    void noRoversGivesNoResult() {
        assertEquals(List.of(), new Simulation(new SimulationInput(new Plateau(5, 5), List.of())).run());
    }
}
