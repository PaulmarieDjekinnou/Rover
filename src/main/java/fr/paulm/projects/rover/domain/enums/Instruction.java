package fr.paulm.projects.rover.domain.enums;

import java.util.Arrays;
import java.util.Optional;

// The commands a rover can execute, each with the letter that writes it in an input (L, R and M).
public enum Instruction {
    LEFT('L'), RIGHT('R'), MOVE('M');

    private final char code;

    Instruction(char code) {
        this.code = code;
    }

    // The instruction written with this letter, if any. The caller decides how to report an unknown letter.
    public static Optional<Instruction> fromCode(char code) {
        return Arrays.stream(values())
                .filter(instruction -> instruction.code == code)
                .findFirst();
    }
}
