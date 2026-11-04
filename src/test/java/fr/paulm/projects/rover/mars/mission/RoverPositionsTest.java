package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;
import fr.paulm.projects.rover.domain.simulation.RoverMission;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fr.paulm.projects.rover.mars.mission.LandingTest.PLANET;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoverPositionsTest {

    // The small planet of LandingTest: 8 columns of 45 degrees, rows centered on 67.5S, 22.5S, 22.5N and 67.5N
    private static RoverPositions landedAt(Position cell, Direction heading, Coordinates point) {
        RoverPlan plan = new RoverPlan("Rover", new LandingSite(point, Ellipse.NONE), heading, List.of());
        Rover rover = new Rover(cell, heading);
        Deployment deployment = new Deployment(PLANET, List.of(new Touchdown(plan, point, cell, Optional.empty())),
                new SimulationInput(PLANET.grid(), Terrain.FLAT,
                        false, List.of(new RoverMission(rover, List.of()))));
        return new RoverPositions(deployment);
    }

    private static void moveTo(RoverPositions positions, int x, int y, Direction direction) {
        positions.onTurn(new Turn(1, List.of(new Rover(new Position(x, y), direction)), Set.of()));
    }

    private static void assertAt(double latitude, double longitude, Coordinates actual) {
        assertEquals(latitude, actual.latitude(), 1e-9);
        assertEquals(longitude, actual.longitude(), 1e-9);
    }

    @Test
    void aRoverStartsWhereItTouchedDown() {
        assertAt(25, 280, landedAt(new Position(6, 2), Direction.W, new Coordinates(25, 280)).of(0));
    }

    @Test
    void aRoverKeepsItsPlaceInItsCellFromCellToCell() {
        // 2.5 degrees north and 10 degrees west of the center of (6, 2), which is 22.5N 292.5E
        RoverPositions positions = landedAt(new Position(6, 2), Direction.W, new Coordinates(25, 282.5));
        moveTo(positions, 5, 2, Direction.W);
        assertAt(25, 237.5, positions.of(0));
        // It turns south on the spot, then moves one row down
        moveTo(positions, 5, 2, Direction.S);
        moveTo(positions, 5, 1, Direction.S);
        assertAt(-20, 237.5, positions.of(0));
    }

    @Test
    void aTurnDoesNotMoveTheRover() {
        RoverPositions positions = landedAt(new Position(6, 2), Direction.W, new Coordinates(25, 282.5));
        moveTo(positions, 6, 2, Direction.N);
        assertAt(25, 282.5, positions.of(0));
    }

    @Test
    void crossingAPoleLeadsToTheOtherSideOfIt() {
        // 5 degrees north of the center of (1, 3), which is 67.5N 67.5E: the rover is between that center and the pole
        RoverPositions positions = landedAt(new Position(1, 3), Direction.N, new Coordinates(72.5, 70.5));
        moveTo(positions, 5, 3, Direction.S);
        // Beyond the pole, it is between the pole and the center of (5, 3) on the other side, farther from the pole
        assertAt(62.5, 250.5, positions.of(0));
        // Crossing the pole back brings it where it started
        moveTo(positions, 1, 3, Direction.N);
        assertAt(72.5, 70.5, positions.of(0));
    }
}
