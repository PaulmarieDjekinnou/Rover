package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static fr.paulm.projects.rover.domain.simulation.TestMissions.CLIFF_EAST_OF_COLUMN_1;
import static fr.paulm.projects.rover.domain.simulation.TestMissions.mission;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockSimulationTest {

    private final Simulation simulation = new ClockSimulation();

    // A move east takes 3 minutes, any other move 1 minute, a turn 30 seconds; the cliff blocks column 1 to 2
    private final Terrain timed = new Terrain() {
        @Override
        public boolean canDrive(Position from, Position to) {
            return CLIFF_EAST_OF_COLUMN_1.canDrive(from, to);
        }

        @Override
        public Duration driveTime(Position from, Position to) {
            return to.x() > from.x() ? Duration.ofMinutes(3) : Duration.ofMinutes(1);
        }

        @Override
        public Duration turnTime() {
            return Duration.ofSeconds(30);
        }
    };

    private List<Turn> history(SimulationInput input) {
        List<Turn> turns = new ArrayList<>();
        simulation.run(input, turns::add);
        return turns;
    }

    @Test
    void withoutDurationsItIsTheStepByStepMode() {
        // The step-by-step test cases: rovers in each other's way, finished rovers, cells freed in the same turn
        List<List<RoverMission>> cases = List.of(
                List.of(mission(0, 0, Direction.N, "MMM"), mission(1, 1, Direction.W, "M")),
                List.of(mission(0, 0, Direction.N, "M"), mission(0, 3, Direction.S, "MMM")),
                List.of(mission(1, 0, Direction.N, "M"), mission(0, 0, Direction.E, "M")),
                List.of(mission(0, 0, Direction.E, "M"), mission(1, 0, Direction.N, "M")),
                List.of(mission(1, 2, Direction.N, "LMLMLMLMM"), mission(3, 3, Direction.E, "MMRMMRMRRM")));
        for (List<RoverMission> missions : cases) {
            SimulationInput input = new SimulationInput(new Plateau(5, 5), missions);
            assertEquals(new StepByStepSimulation().run(input), simulation.run(input));
        }
    }

    @Test
    void instructionsEndInTimeOrder() {
        // The first rover drives east (3 min), the second one north twice (1 min each)
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, false, List.of(
                mission(2, 0, Direction.E, "M"),
                mission(0, 2, Direction.N, "MM")));
        List<Turn> turns = history(input);
        assertEquals(List.of(Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(3)),
                turns.stream().map(Turn::time).toList());
        assertEquals(List.of(1, 2, 3), turns.stream().map(Turn::number).toList());
        assertEquals("3 0 E", turns.get(2).rovers().get(0).toString());
    }

    @Test
    void eachRoverHasTheTimeOfItsOwnInstructions() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, false, List.of(
                mission(2, 0, Direction.E, "ML"),
                mission(0, 2, Direction.N, "MRM")));
        List<MissionResult> results = simulation.run(input);
        assertEquals(Duration.ofSeconds(210), results.get(0).drivingTime());
        assertEquals(Duration.ofSeconds(270), results.get(1).drivingTime());
    }

    @Test
    void aMoveTheTerrainBlocksTakesNoTime() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, false, List.of(
                mission(1, 0, Direction.E, "ML")));
        List<Turn> turns = history(input);
        assertEquals(List.of(Duration.ZERO, Duration.ofSeconds(30)), turns.stream().map(Turn::time).toList());
        assertEquals(new MissionResult(new Rover(new Position(1, 0), Direction.N), 0, 1, 0, Duration.ofSeconds(30)),
                simulation.run(input).getFirst());
    }
}
