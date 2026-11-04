package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.mars.rover.Mobility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static fr.paulm.projects.rover.mars.mission.LandingTest.PLANET;
import static fr.paulm.projects.rover.mars.mission.LandingTest.RULES;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MarsMissionTest {

    private static RoverPlan plan(String name, int x, int y) {
        return new RoverPlan(name, new LandingSite(PLANET.centerOf(new Position(x, y)), Ellipse.NONE), Direction.N, List.of());
    }

    private static MarsMission mission(long seed, RoverPlan... rovers) {
        return new MarsMission(PLANET, Mobility.DEFAULT, RULES, seed, List.of(rovers));
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
    void aRoverLandingWhereItCannotDriveIsLost() {
        // The target is not checked here: the ellipse can bring a rover to such a cell
        Deployment deployment = mission(1, plan("Cliff", 3, 1)).land();
        assertEquals(Optional.of(LandingFailure.NO_WAY_OUT), deployment.touchdowns().get(0).failure());
        assertEquals(List.of(), deployment.input().missions());
    }

    @Test
    void theSameSeedGivesTheSameLandings() {
        RoverPlan wide = new RoverPlan("Wide", new LandingSite(PLANET.centerOf(new Position(6, 2)), new Ellipse(5, 3, 45)),
                Direction.E, List.of());
        assertEquals(mission(42, wide).land().touchdowns(), mission(42, wide).land().touchdowns());
    }
}
