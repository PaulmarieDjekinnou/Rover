package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoverMissionTest {

    @Test
    void instructionListCannotBeChangedAfterConstruction() {
        List<Instruction> instructions = new ArrayList<>(List.of(Instruction.M));
        RoverMission mission = new RoverMission(new Rover(0, 0, Direction.N), instructions);

        instructions.add(Instruction.L);
        assertEquals(List.of(Instruction.M), mission.instructions());
        assertThrows(UnsupportedOperationException.class, () -> mission.instructions().add(Instruction.R));
    }
}
