package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.rover.Mobility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static fr.paulm.projects.rover.mars.mission.LandingTest.PLANET;
import static fr.paulm.projects.rover.mars.mission.LandingTest.RULES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanetMissionTest {

    private static RoverPlan plan(String name, int x, int y) {
        return new RoverPlan(name, new LandingSite(PLANET.centerOf(new Position(x, y)), Ellipse.NONE), Direction.N, List.of());
    }

    private static PlanetMission mission(long seed, RoverPlan... rovers) {
        return new PlanetMission(PLANET, Mobility.DEFAULT, RULES, seed, List.of(rovers));
    }

    @Test
    void roversLandInTheirTargetCellFacingTheirHeading() {
        Deployment deployment = mission(1, plan("One", 6, 2), plan("Two", 5, 1)).land();
        assertEquals(List.of(new Position(6, 2), new Position(5, 1)),
                deployment.touchdowns().stream().map(Touchdown::cell).toList());
        assertEquals(List.of(new Rover(new Position(6, 2), Direction.N), new Rover(new Position(5, 1), Direction.N)),
                deployment.input().missions().stream().map(mission -> mission.rover()).toList());
    }

    @Test
    void aRoverLandingOnAnotherOneIsLost() {
        Deployment deployment = mission(1, plan("One", 6, 2), plan("Two", 6, 2), plan("Three", 5, 1)).land();
        assertEquals(Optional.of(LandingFailure.ON_ANOTHER_ROVER), deployment.touchdowns().get(1).failure());
        // Only the rovers that landed take part in the simulation, in input order
        assertEquals(List.of("One", "Three"), deployment.landed().stream().map(touchdown -> touchdown.plan().name()).toList());
        assertEquals(2, deployment.input().missions().size());
    }

    @Test
    void roversCanLandInTheSameCellAFewKilometersApart() {
        Coordinates center = PLANET.centerOf(new Position(6, 2));
        RoverPlan near = new RoverPlan("Near", new LandingSite(center.shiftedBy(2_000, 0, PLANET.radiusMeters()), Ellipse.NONE),
                Direction.N, List.of());
        Deployment deployment = mission(1, plan("One", 6, 2), near).land();
        assertEquals(List.of(new Position(6, 2), new Position(6, 2)),
                deployment.touchdowns().stream().map(Touchdown::cell).toList());
        assertEquals(2, deployment.landed().size());
        assertFalse(deployment.input().oneRoverPerCell());
    }

    @Test
    void aRoverLandingWhereItCannotDriveIsLost() {
        // The rover aims at a flat site two cells east of the peak, with a 40 km wide ellipse:
        // the first seed that brings it onto the peak loses it
        RoverPlan wide = new RoverPlan("Unlucky", new LandingSite(PLANET.centerOf(new Position(5, 1)), new Ellipse(40, 2, 90)),
                Direction.N, List.of());
        Deployment deployment = LongStream.range(0, 1_000)
                .mapToObj(seed -> mission(seed, wide).land())
                .filter(landing -> landing.touchdowns().getFirst().cell().equals(new Position(3, 1)))
                .findFirst().orElseThrow();
        assertEquals(Optional.of(LandingFailure.NO_WAY_OUT), deployment.touchdowns().getFirst().failure());
        assertEquals(List.of(), deployment.input().missions());
    }

    @Test
    void theMissionRefusesASiteTheLandingRulesRefuse() {
        assertThrows(InvalidInputException.class, () -> mission(1, plan("Peak", 3, 1)));
    }

    @Test
    void theMissionRefusesTwoRoversWithTheSameName() {
        assertThrows(InvalidInputException.class, () -> mission(1, plan("One", 6, 2), plan("One", 5, 1)));
    }

    @Test
    void theSameSeedGivesTheSameLandings() {
        RoverPlan wide = new RoverPlan("Wide", new LandingSite(PLANET.centerOf(new Position(6, 2)), new Ellipse(5, 3, 45)),
                Direction.E, List.of());
        assertEquals(mission(42, wide).land().touchdowns(), mission(42, wide).land().touchdowns());
    }

    @Test
    void aRoverKeepsItsPlaceInsideItsCellFromCellToCell() {
        // Touches down 2 km north and 1 km east of the center of (6, 2)
        Coordinates center = PLANET.centerOf(new Position(6, 2));
        Coordinates point = center.shiftedBy(2_000, 1_000, PLANET.radiusMeters());
        RoverPlan plan = new RoverPlan("Offset", new LandingSite(point, Ellipse.NONE), Direction.N, List.of());
        Touchdown touchdown = mission(1, plan).land().touchdowns().getFirst();
        // In its landing cell, the rover is exactly where it touched down
        Coordinates there = touchdown.positionIn(new Position(6, 2), PLANET);
        assertEquals(point.latitude(), there.latitude(), 1e-9);
        assertEquals(point.longitude(), there.longitude(), 1e-9);
        // In the next cell to the west, it is 2 km north and 1 km east of that cell's center
        Coordinates west = touchdown.positionIn(new Position(5, 2), PLANET);
        Coordinates westCenter = PLANET.centerOf(new Position(5, 2));
        assertEquals(2_000, westCenter.distanceTo(new Coordinates(west.latitude(), westCenter.longitude()), PLANET.radiusMeters()), 1);
        assertEquals(1_000, westCenter.distanceTo(new Coordinates(westCenter.latitude(), west.longitude()), PLANET.radiusMeters()), 1);
    }
}
