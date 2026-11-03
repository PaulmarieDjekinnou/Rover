package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.LogCapture;
import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationTest {

    private LogCapture logs;

    @BeforeEach
    void captureLogs() {
        logs = LogCapture.start(Simulation.class);
    }

    @AfterEach
    void stopCapture() {
        logs.stop();
    }

    @Test
    void logsFinalPositionOfEachRoverInOrder() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(1, 2), Direction.N), TestInstructions.of("LMLMLMLMM")),
                new RoverMission(new Rover(new Position(3, 3), Direction.E), TestInstructions.of("MMRMMRMRRM"))));
        new Simulation(input).run();
        assertEquals(List.of("1 3 N", "5 1 E"), logs.messages());
    }

    @Test
    void roversShareThePlateauLimits() {
        SimulationInput input = new SimulationInput(new Plateau(2, 2), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MMMM")),
                new RoverMission(new Rover(new Position(1, 0), Direction.E), TestInstructions.of("MMMM"))));
        new Simulation(input).run();
        assertEquals(List.of("0 2 N", "2 0 E"), logs.messages());
    }

    // Collisions

    @Test
    void roverCannotMoveOntoTheFinalPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MMM")),
                new RoverMission(new Rover(new Position(0, 5), Direction.S), TestInstructions.of("MMM"))));
        new Simulation(input).run();
        // The first rover ends in (0, 3): the second one stops just above it
        assertEquals(List.of("0 3 N", "0 4 S"), logs.messages());
    }

    @Test
    void roverCannotMoveOntoTheStartingPositionOfANextRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.E), TestInstructions.of("MMMM")),
                new RoverMission(new Rover(new Position(3, 0), Direction.N), TestInstructions.of("M"))));
        new Simulation(input).run();
        // The second rover has not moved yet when the first one runs: it blocks the cell (3, 0)
        assertEquals(List.of("2 0 E", "3 1 N"), logs.messages());
    }

    @Test
    void roverCanMoveOntoTheStartingPositionOfAPreviousRover() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(0, 0), Direction.N), TestInstructions.of("MM")),
                new RoverMission(new Rover(new Position(1, 0), Direction.W), TestInstructions.of("M"))));
        new Simulation(input).run();
        // The first rover has left (0, 0): the cell is free again
        assertEquals(List.of("0 2 N", "0 0 W"), logs.messages());
    }

    @Test
    void runCanBeReplayedWithTheSameResult() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(new Position(1, 2), Direction.N), TestInstructions.of("LMLMLMLMM"))));
        Simulation simulation = new Simulation(input);
        simulation.run();
        simulation.run();
        assertEquals(List.of("1 3 N", "1 3 N"), logs.messages());
    }

    @Test
    void noRoversLogsNothing() {
        new Simulation(new SimulationInput(new Plateau(5, 5), List.of())).run();
        assertEquals(List.of(), logs.messages());
    }
}
