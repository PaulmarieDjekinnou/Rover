package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationInputTest {

    private final Plateau plateau = new Plateau(5, 5);

    private static RoverMission missionAt(int x, int y) {
        return new RoverMission(new Rover(new Position(x, y), Direction.N), List.of(Instruction.MOVE));
    }

    @Test
    void acceptsRoverStartingOnTheEdgeOfThePlateau() {
        // The plateau boundaries themselves are tested in PlateauTest.
        assertDoesNotThrow(() -> new SimulationInput(plateau, List.of(missionAt(5, 5))));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "0, -1", "6, 0", "0, 6"})
    void rejectsRoverStartingOutsidePlateau(int x, int y) {
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> new SimulationInput(plateau, List.of(missionAt(1, 1), missionAt(x, y))));
        assertEquals("Rover starts outside the plateau: \"" + x + " " + y + "\"", e.getMessage());
    }

    @Test
    void rejectsTwoRoversStartingOnTheSameCell() {
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> new SimulationInput(plateau, List.of(missionAt(1, 1), missionAt(2, 2), missionAt(1, 1))));
        assertEquals("Two rovers start on the same cell: \"1 1\"", e.getMessage());
    }

    @Test
    void missionListCannotBeChangedAfterConstruction() {
        List<RoverMission> missions = new ArrayList<>(List.of(missionAt(1, 1)));
        SimulationInput input = new SimulationInput(plateau, missions);

        missions.add(missionAt(10, 10));
        assertEquals(1, input.missions().size());
        assertThrows(UnsupportedOperationException.class, () -> input.missions().add(missionAt(2, 2)));
    }
}
