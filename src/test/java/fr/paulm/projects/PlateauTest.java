package fr.paulm.projects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateauTest {

    // Plateau non carré : une inversion largeur / hauteur ferait échouer les coins (5, 0) et (5, 3).
    private final Plateau plateau = new Plateau(5, 3);

    @ParameterizedTest
    @CsvSource({"0, 0", "5, 0", "0, 3", "5, 3"})
    void containsItsFourCorners(int x, int y) {
        assertTrue(plateau.contains(x, y));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1", "6, 0", "0, 4"})
    void doesNotContainPointsJustOutsideEachSide(int x, int y) {
        assertFalse(plateau.contains(x, y));
    }

    @ParameterizedTest
    @CsvSource({"-1, 3", "5, -1"})
    void rejectsNegativeSize(int maxX, int maxY) {
        assertThrows(IllegalArgumentException.class, () -> new Plateau(maxX, maxY));
    }
}
