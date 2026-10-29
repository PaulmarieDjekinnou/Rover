package fr.paulm.projects.rover;

// Exit codes of the program, one per kind of error, so that a script can react to the cause.
public enum ExitCode {
    SUCCESS(0),
    USAGE_ERROR(2),
    INVALID_INPUT(3),
    UNREADABLE_FILE(4);

    private final int value;

    ExitCode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
