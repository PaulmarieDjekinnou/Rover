package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Instruction;

import java.util.List;

public final class TestInstructions {

    private TestInstructions() {
    }

    // "LMR" → [L, M, R], pour écrire les instructions des tests de façon lisible.
    public static List<Instruction> of(String letters) {
        return letters.chars()
                .mapToObj(c -> Instruction.valueOf(String.valueOf((char) c)))
                .toList();
    }
}
