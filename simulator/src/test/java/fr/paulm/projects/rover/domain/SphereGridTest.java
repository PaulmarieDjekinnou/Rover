package fr.paulm.projects.rover.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static fr.paulm.projects.rover.domain.Direction.E;
import static fr.paulm.projects.rover.domain.Direction.N;
import static fr.paulm.projects.rover.domain.Direction.S;
import static fr.paulm.projects.rover.domain.Direction.W;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SphereGridTest {

    // 8 longitudes and 4 latitudes: rows 0 (next to the south pole) to 3 (next to the north pole)
    private final SphereGrid sphere = new SphereGrid(8, 4);

    private Rover run(Rover rover, String instructions) {
        Rover current = rover;
        for (char code : instructions.toCharArray()) {
            current = current.apply(Instruction.fromCode(code).orElseThrow(), sphere);
        }
        return current;
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "7, 0", "0, 3", "7, 3"})
    void containsItsFourCorners(int x, int y) {
        assertTrue(sphere.contains(new Position(x, y)));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "8, 0", "0, -1", "0, 4"})
    void doesNotContainCellsOutsideTheGrid(int x, int y) {
        assertFalse(sphere.contains(new Position(x, y)));
    }

    @ParameterizedTest
    @CsvSource({"0, 4", "7, 4", "8, 0", "-2, 4"})
    void rejectsAnOddOrEmptyGrid(int columns, int rows) {
        assertThrows(InvalidInputException.class, () -> new SphereGrid(columns, rows));
    }

    @ParameterizedTest
    @CsvSource({"3, 1, N, 3, 2, N", "3, 1, S, 3, 0, S", "3, 1, E, 4, 1, E", "3, 1, W, 2, 1, W"})
    void insideTheGridNextIsTheCellAhead(int x, int y, Direction direction, int expectedX, int expectedY, Direction expected) {
        assertEquals(new Heading(new Position(expectedX, expectedY), expected), sphere.next(new Position(x, y), direction));
    }

    @ParameterizedTest
    @CsvSource({"7, 2, E, 0, 2", "0, 2, W, 7, 2"})
    void longitudeWrapsAround(int x, int y, Direction direction, int expectedX, int expectedY) {
        assertEquals(new Heading(new Position(expectedX, expectedY), direction), sphere.next(new Position(x, y), direction));
    }

    // Crossing a pole leads half a turn of longitude away, on the same row, going the other way
    @ParameterizedTest
    @CsvSource({"1, 3, N, 5, 3, S", "6, 3, N, 2, 3, S", "1, 0, S, 5, 0, N", "6, 0, S, 2, 0, N"})
    void crossingAPoleLeadsToTheOppositeLongitudeAndTurnsAround(int x, int y, Direction direction,
                                                                int expectedX, int expectedY, Direction expected) {
        assertEquals(new Heading(new Position(expectedX, expectedY), expected), sphere.next(new Position(x, y), direction));
    }

    @Test
    void roverCrossingTheNorthPoleComesBackFacingSouth() {
        assertEquals(new Rover(new Position(5, 3), S), run(new Rover(new Position(1, 2), N), "MM"));
        assertEquals(new Rover(new Position(5, 2), S), run(new Rover(new Position(1, 2), N), "MMM"));
    }

    @Test
    void roverDrivingBackwardsAcrossAPoleIsTurnedAroundToo() {
        // Facing north, backing over the south pole: it arrives on the other side, still backing away, now facing south
        assertEquals(new Rover(new Position(5, 0), S), run(new Rover(new Position(1, 0), N), "B"));
        assertEquals(new Rover(new Position(5, 1), S), run(new Rover(new Position(1, 0), N), "BB"));
    }

    @Test
    void goingAllTheWayAroundAMeridianComesBackToTheStart() {
        // 4 rows: 8 moves cross both poles and bring the rover back, facing north again
        assertEquals(new Rover(new Position(1, 1), N), run(new Rover(new Position(1, 1), N), "MMMMMMMM"));
    }

    @Test
    void goingAllTheWayAroundTheEquatorComesBackToTheStart() {
        assertEquals(new Rover(new Position(1, 1), E), run(new Rover(new Position(1, 1), E), "MMMMMMMM"));
    }
}
