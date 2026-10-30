package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.List;

public final class TestInstructions {

    private TestInstructions() {
    }

    // "LMR" → [LEFT, MOVE, RIGHT], to write test instructions in a readable way, with the letters of the text format.
    public static List<Instruction> of(String letters) {
        return letters.chars()
                .mapToObj(c -> Instruction.fromCode((char) c).orElseThrow())
                .toList();
    }
}
