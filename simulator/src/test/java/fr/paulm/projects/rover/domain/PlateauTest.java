package fr.paulm.projects.rover.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateauTest {

    // Non-square plateau: swapping width and height would make the (5, 0) and (5, 3) corners fail.
    private final Plateau plateau = new Plateau(5, 3);

    @ParameterizedTest
    @CsvSource({"0, 0", "5, 0", "0, 3", "5, 3"})
    void containsItsFourCorners(int x, int y) {
        assertTrue(plateau.contains(new Position(x, y)));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1", "6, 0", "0, 4"})
    void doesNotContainPointsJustOutsideEachSide(int x, int y) {
        assertFalse(plateau.contains(new Position(x, y)));
    }

    @ParameterizedTest
    @CsvSource({"-1, 3", "5, -1"})
    void rejectsNegativeSize(int maxX, int maxY) {
        assertThrows(InvalidInputException.class, () -> new Plateau(maxX, maxY));
    }

    // The plateau is a torus: positions beyond an edge come back on the opposite edge
    @ParameterizedTest
    @CsvSource({"2, 1, 2, 1", "6, 1, 0, 1", "-1, 1, 5, 1", "2, 4, 2, 0", "2, -1, 2, 3", "6, 4, 0, 0"})
    void wrapBringsPositionsBackOnThePlateau(int x, int y, int expectedX, int expectedY) {
        assertEquals(new Position(expectedX, expectedY), plateau.wrap(new Position(x, y)));
    }

    // next is the cell ahead, wrapped around; the direction never changes on a plateau
    @ParameterizedTest
    @CsvSource({"2, 1, N, 2, 2", "5, 1, E, 0, 1", "2, 0, S, 2, 3", "0, 1, W, 5, 1"})
    void nextIsTheWrappedCellAheadWithTheSameDirection(int x, int y, Direction direction, int expectedX, int expectedY) {
        assertEquals(new Heading(new Position(expectedX, expectedY), direction), plateau.next(new Position(x, y), direction));
    }

    @Test
    void wrapWorksOnTheLargestPossiblePlateau() {
        Plateau huge = new Plateau(Integer.MAX_VALUE, 0);
        // Integer.MAX_VALUE + 1 overflows to Integer.MIN_VALUE: it must still wrap to 0
        assertEquals(new Position(0, 0), huge.wrap(new Position(Integer.MIN_VALUE, 0)));
    }
}
