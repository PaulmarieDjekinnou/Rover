package fr.paulm.projects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateauTest {

    private final Plateau plateau = new Plateau(5, 5);

    @ParameterizedTest
    @CsvSource({"0, 0", "5, 5", "0, 5", "5, 0", "2, 3"})
    void containsPointsInsideAndOnTheEdges(int x, int y) {
        assertTrue(plateau.contains(x, y));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1", "6, 0", "0, 6", "6, 6", "-1, -1"})
    void doesNotContainPointsOutside(int x, int y) {
        assertFalse(plateau.contains(x, y));
    }

    @Test
    void zeroSizedPlateauContainsOnlyOrigin() {
        Plateau tiny = new Plateau(0, 0);
        assertTrue(tiny.contains(0, 0));
        assertFalse(tiny.contains(1, 0));
        assertFalse(tiny.contains(0, 1));
    }

    @Test
    void nonSquarePlateau() {
        Plateau wide = new Plateau(10, 2);
        assertTrue(wide.contains(10, 2));
        assertFalse(wide.contains(2, 10));
    }

    @ParameterizedTest
    @CsvSource({"-1, 5", "5, -1", "-1, -1"})
    void rejectsNegativeSize(int maxX, int maxY) {
        assertThrows(IllegalArgumentException.class, () -> new Plateau(maxX, maxY));
    }

    @Test
    void acceptsZeroSize() {
        assertDoesNotThrow(() -> new Plateau(0, 0));
    }
}
