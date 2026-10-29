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
                new RoverMission(new Rover(1, 2, Direction.N), TestInstructions.of("LMLMLMLMM")),
                new RoverMission(new Rover(3, 3, Direction.E), TestInstructions.of("MMRMMRMRRM"))));
        new Simulation(input).run();
        assertEquals(List.of("1 3 N", "5 1 E"), logs.messages());
    }

    @Test
    void roversShareThePlateauLimits() {
        SimulationInput input = new SimulationInput(new Plateau(2, 2), List.of(
                new RoverMission(new Rover(0, 0, Direction.N), TestInstructions.of("MMMM")),
                new RoverMission(new Rover(0, 0, Direction.E), TestInstructions.of("MMMM"))));
        new Simulation(input).run();
        assertEquals(List.of("0 2 N", "2 0 E"), logs.messages());
    }

    @Test
    void runCanBeReplayedWithTheSameResult() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverMission(new Rover(1, 2, Direction.N), TestInstructions.of("LMLMLMLMM"))));
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
