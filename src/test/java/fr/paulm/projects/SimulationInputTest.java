package fr.paulm.projects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationInputTest {

    private final Plateau plateau = new Plateau(5, 5);

    private static RoverInstructions rover(int x, int y) {
        return new RoverInstructions(new Rover(x, y, Direction.N), "M");
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "5, 5", "0, 5", "5, 0"})
    void acceptsRoversOnThePlateau(int x, int y) {
        assertDoesNotThrow(() -> new SimulationInput(plateau, List.of(rover(x, y))));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1", "6, 0", "0, 6"})
    void rejectsRoverStartingOutsidePlateau(int x, int y) {
        String message = assertThrows(IllegalArgumentException.class,
                () -> new SimulationInput(plateau, List.of(rover(1, 1), rover(x, y)))).getMessage();
        assertEquals("Le rover démarre hors du plateau : " + x + " " + y, message);
    }
}
