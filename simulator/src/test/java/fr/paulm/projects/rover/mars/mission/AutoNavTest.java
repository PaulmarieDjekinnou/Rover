package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import fr.paulm.projects.rover.mars.rover.Mobility;
import fr.paulm.projects.rover.mars.rover.RoutePlanner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoNavTest {

    // A flat planet of 16 x 8 cells, a quarter of them under rock fields that the orbital map does not show
    private static Planet rocky(long seed) {
        return TestPlanets.planet(10_000, 100,
                "0000000000000000", "0000000000000000", "0000000000000000", "0000000000000000",
                "0000000000000000", "0000000000000000", "0000000000000000", "0000000000000000")
                .withRockFields(0.25, seed);
    }

    // Drives the rover with its pilot until it has nothing left to do; never into a rock field
    private static Rover drive(AutoNav pilot, Planet planet, Rover start) {
        Rover rover = start;
        for (Optional<Instruction> next = pilot.next(rover); next.isPresent(); next = pilot.next(rover)) {
            rover = rover.apply(next.get(), planet.grid());
            assertTrue(!planet.rocky(rover.position()), "drove into a rock field at " + rover.position());
        }
        return rover;
    }

    @Test
    void theRoverFindsRockFieldsOnItsWayAndGoesAroundThem() {
        int replans = 0;
        for (long seed = 1; seed <= 20; seed++) {
            Planet planet = rocky(seed);
            Position start = new Position(1, 4);
            Position goal = new Position(12, 4);
            if (planet.rocky(start) || planet.rocky(goal)) {
                continue;
            }
            AutoNav pilot = new AutoNav(planet, new RoutePlanner(planet, Mobility.DEFAULT), List.of(planet.centerOf(goal)));
            Rover end = drive(pilot, planet, new Rover(start, Direction.E));
            if (pilot.missedWaypoints() == 0) {
                assertEquals(goal, end.position());
            }
            replans += pilot.replans();
        }
        // On a quarter of rocky cells, the straight routes of the orbital map meet rock fields
        assertTrue(replans > 0);
    }

    @Test
    void withoutRockFieldsTheRouteIsThatOfTheOrbitalMap() {
        Planet planet = rocky(1).withRockFields(0, 1);
        RoutePlanner planner = new RoutePlanner(planet, Mobility.DEFAULT);
        AutoNav pilot = new AutoNav(planet, planner, List.of(planet.centerOf(new Position(5, 4))));
        Rover end = drive(pilot, planet, new Rover(new Position(1, 4), Direction.E));
        assertEquals(new Position(5, 4), end.position());
        assertEquals(0, pilot.replans());
    }
}
