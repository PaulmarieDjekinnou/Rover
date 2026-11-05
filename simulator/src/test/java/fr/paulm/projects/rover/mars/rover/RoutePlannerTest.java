package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.TestInstructions;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutePlannerTest {

    // 8 x 4 cells on a radius of 10 km, steps of 1 km: the 9 km peak in (3, 1) cannot be driven onto
    private final Planet planet = TestPlanets.planet(10_000, 1_000,
            "00000000",
            "00000000",
            "00090000",
            "00000000");
    private final RoutePlanner planner = new RoutePlanner(planet, Mobility.DEFAULT);

    private Rover end(Rover start, List<Instruction> instructions) {
        Rover rover = start;
        for (Instruction instruction : instructions) {
            rover = rover.apply(instruction, planet.grid());
        }
        return rover;
    }

    @Test
    void aStraightRouteNeedsNoTurn() {
        Optional<List<Instruction>> route = planner.route(new Rover(new Position(1, 1), Direction.N),
                planet.centerOf(new Position(1, 3)));
        assertEquals(Optional.of(TestInstructions.of("MM")), route);
    }

    @Test
    void theRouteGoesAroundThePeak() {
        Rover start = new Rover(new Position(2, 1), Direction.E);
        List<Instruction> route = planner.route(start, planet.centerOf(new Position(4, 1))).orElseThrow();
        assertEquals(new Position(4, 1), end(start, route).position());
        // It never drives onto the peak
        Rover rover = start;
        for (Instruction instruction : route) {
            rover = rover.apply(instruction, planet.grid());
            assertTrue(!rover.position().equals(new Position(3, 1)));
        }
    }

    @Test
    void thePeakItselfCannotBeReached() {
        assertEquals(Optional.empty(),
                planner.route(new Rover(new Position(0, 1), Direction.E), planet.centerOf(new Position(3, 1))));
    }

    @Test
    void theRouteMayCrossAPole() {
        // From (1, 3) to (5, 3): across the north pole is one move, the other ways are longer
        Optional<List<Instruction>> route = planner.route(new Rover(new Position(1, 3), Direction.N),
                planet.centerOf(new Position(5, 3)));
        assertEquals(Optional.of(TestInstructions.of("M")), route);
    }

    @Test
    void aRoverAlreadyThereNeedsNothing() {
        assertEquals(Optional.of(List.of()),
                planner.route(new Rover(new Position(6, 2), Direction.S), planet.centerOf(new Position(6, 2))));
    }
}
