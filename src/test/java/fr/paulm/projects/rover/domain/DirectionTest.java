package fr.paulm.projects.rover.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectionTest {

    private final Position origin = new Position(2, 2);

    @ParameterizedTest
    @CsvSource({"N, 2, 3", "E, 3, 2", "S, 2, 1", "W, 1, 2"})
    void aheadIsOneStepInTheDirection(Direction direction, int expectedX, int expectedY) {
        assertEquals(new Position(expectedX, expectedY), direction.ahead(origin));
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void behindIsAheadOfTheOppositeDirection(Direction direction) {
        Direction opposite = direction.right().right();
        assertEquals(opposite.ahead(origin), direction.behind(origin));
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void fromCodeFindsEachDirection(Direction direction) {
        assertEquals(Optional.of(direction), Direction.fromCode(direction.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "n", "NORTH", ""})
    void fromCodeFindsNothingForAnUnknownCode(String code) {
        assertEquals(Optional.empty(), Direction.fromCode(code));
    }

    @ParameterizedTest
    @CsvSource({"N, S", "E, W", "S, N", "W, E"})
    void oppositeIsTheDirectionAfterAHalfTurn(Direction direction, Direction opposite) {
        assertEquals(opposite, direction.opposite());
    }
}
