package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionTest {

    @Test
    void shiftedByReturnsANewPositionAndLeavesTheOriginalUnchanged() {
        Position start = new Position(1, 2);
        assertEquals(new Position(3, 1), start.shiftedBy(2, -1));
        assertEquals(new Position(1, 2), start);
    }

    @Test
    void toStringFormatsTheCoordinates() {
        assertEquals("1 2", new Position(1, 2).toString());
    }

    // The neighbouring cell in each direction
    @ParameterizedTest
    @CsvSource({"N, 2, 3", "E, 3, 2", "S, 2, 1", "W, 1, 2"})
    void neighbourInEachDirection(Direction direction, int expectedX, int expectedY) {
        assertEquals(new Position(expectedX, expectedY), direction.ahead(new Position(2, 2)));
    }
}
