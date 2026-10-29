package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.List;

public final class TestInstructions {

    private TestInstructions() {
    }

    // "LMR" → [L, M, R], to write test instructions in a readable way.
    public static List<Instruction> of(String letters) {
        return letters.chars()
                .mapToObj(c -> Instruction.valueOf(String.valueOf((char) c)))
                .toList();
    }
}
