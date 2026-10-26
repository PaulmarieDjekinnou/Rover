package fr.paulm.projects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectionTest {

    @ParameterizedTest
    @CsvSource({"N, W", "W, S", "S, E", "E, N"})
    void leftRotatesCounterClockwise(Direction from, Direction expected) {
        assertEquals(expected, from.left());
    }

    @ParameterizedTest
    @CsvSource({"N, E", "E, S", "S, W", "W, N"})
    void rightRotatesClockwise(Direction from, Direction expected) {
        assertEquals(expected, from.right());
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void fourTurnsInSameDirectionReturnToStart(Direction d) {
        assertEquals(d, d.left().left().left().left());
        assertEquals(d, d.right().right().right().right());
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void leftThenRightCancelsOut(Direction d) {
        assertEquals(d, d.left().right());
        assertEquals(d, d.right().left());
    }

    @Test
    void vectorsPointTheRightWay() {
        assertEquals(0, Direction.N.dx);
        assertEquals(1, Direction.N.dy);
        assertEquals(1, Direction.E.dx);
        assertEquals(0, Direction.E.dy);
        assertEquals(0, Direction.S.dx);
        assertEquals(-1, Direction.S.dy);
        assertEquals(-1, Direction.W.dx);
        assertEquals(0, Direction.W.dy);
    }
}
