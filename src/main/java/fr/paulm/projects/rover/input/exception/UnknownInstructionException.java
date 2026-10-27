package fr.paulm.projects.rover.input.exception;

public final class UnknownInstructionException extends InputFormatException {
    private final char instruction;

    public UnknownInstructionException(char instruction) {
        super("Instruction inconnue : " + instruction);
        this.instruction = instruction;
    }

    public char instruction() {
        return instruction;
    }
}
