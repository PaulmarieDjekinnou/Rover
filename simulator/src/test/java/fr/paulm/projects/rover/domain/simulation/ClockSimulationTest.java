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
import java.util.Optional;
import java.util.OptionalInt;

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

    @Test
    void aMovingRoverHoldsBothCellsUntilItArrives() {
        // The first rover drives east from (2, 0) to (3, 0) for 3 minutes. Meanwhile, after 1 minute:
        // - the second rover cannot drive into (3, 0), reserved by the first one;
        // - the third rover cannot drive into (2, 0), which the first one has not left yet.
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, true, List.of(
                mission(2, 0, Direction.E, "M"),
                mission(3, 2, Direction.S, "MM"),
                mission(2, 2, Direction.S, "MM")));
        List<MissionResult> results = simulation.run(input);
        assertEquals(new Position(3, 0), results.get(0).finalRover().position());
        assertEquals(new Position(3, 1), results.get(1).finalRover().position());
        assertEquals(1, results.get(1).blockedByRovers());
        assertEquals(new Position(2, 1), results.get(2).finalRover().position());
        assertEquals(1, results.get(2).blockedByRovers());
    }

    @Test
    void aRoverCanDriveIntoACellOnceItIsFree() {
        // The first rover leaves (4, 0) northwards in 1 minute; the second one reaches (3, 0) after 3 minutes
        // and then drives into (4, 0), long freed
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, true, List.of(
                mission(4, 0, Direction.N, "M"),
                mission(2, 0, Direction.E, "MM")));
        List<MissionResult> results = simulation.run(input);
        assertEquals(new Position(4, 1), results.get(0).finalRover().position());
        assertEquals(new Position(4, 0), results.get(1).finalRover().position());
        assertEquals(0, results.get(1).blockedByRovers());
    }

    @Test
    void eachRoverStartsWhenThePaceSays() {
        // The second rover starts 10 minutes after the first one
        Pace lateSecond = new Pace() {
            @Override
            public Timing timing(int rover, Rover from, Rover to, Duration start, Duration busy) {
                return Timing.done(busy, Duration.ZERO);
            }

            @Override
            public Duration start(int rover) {
                return rover == 1 ? Duration.ofMinutes(10) : Duration.ZERO;
            }
        };
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, false, List.of(
                mission(0, 2, Direction.N, "M"),
                mission(4, 2, Direction.N, "M")));
        List<Turn> turns = new ArrayList<>();
        new ClockSimulation(lateSecond).run(input, turns::add);
        assertEquals(List.of(Duration.ofMinutes(1), Duration.ofMinutes(11)), turns.stream().map(Turn::time).toList());
    }

    @Test
    void thePaceCanMakeARoverWait() {
        // The second rover waits 10 minutes before each instruction
        Pace slowSecond = (rover, from, to, start, busy) -> Pace.Timing.done(busy, Duration.ofMinutes(rover == 1 ? 10 : 0));
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, false, List.of(
                mission(0, 2, Direction.N, "M"),
                mission(4, 2, Direction.N, "MM")));
        List<Turn> turns = new ArrayList<>();
        List<MissionResult> results = new ClockSimulation(slowSecond).run(input, turns::add);
        assertEquals(List.of(Duration.ofMinutes(1), Duration.ofMinutes(11), Duration.ofMinutes(22)),
                turns.stream().map(Turn::time).toList());
        assertEquals(Duration.ofMinutes(2), results.get(1).drivingTime());
        assertEquals(Duration.ofMinutes(20), results.get(1).waitingTime());
        assertEquals(Duration.ofMinutes(22), results.get(1).totalTime());
    }

    @Test
    void aRoverThePaceStopsLeavesItsOtherInstructionsUndone() {
        // The rover can carry out its first instruction only; it gives up on the second one after 5 minutes
        Pace oneOnly = (rover, from, to, start, busy) -> start.isZero()
                ? Pace.Timing.done(busy, Duration.ZERO) : Pace.Timing.stoppedAfter(Duration.ofMinutes(5), Duration.ofMinutes(5));
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, false, List.of(
                mission(0, 2, Direction.N, "MMM")));
        MissionResult result = new ClockSimulation(oneOnly).run(input).getFirst();
        assertEquals(new Position(0, 3), result.finalRover().position());
        assertEquals(Optional.of(Duration.ofMinutes(6)), result.stopTime());
        assertEquals(Duration.ofMinutes(5), result.waitingTime());
    }

    @Test
    void eachTurnNamesTheRoverThatActed() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), timed, false, List.of(
                mission(2, 0, Direction.E, "M"),
                mission(0, 2, Direction.N, "MM")));
        // The second rover ends its moves after 1 and 2 minutes, the first one after 3
        assertEquals(List.of(OptionalInt.of(1), OptionalInt.of(1), OptionalInt.of(0)),
                history(input).stream().map(Turn::actor).toList());
    }
}
