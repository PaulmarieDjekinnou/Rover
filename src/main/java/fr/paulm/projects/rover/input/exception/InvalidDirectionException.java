package fr.paulm.projects.rover.input.exception;

public final class InvalidDirectionException extends InputFormatException {
    private final String value;

    public InvalidDirectionException(String value) {
        super("Direction invalide : " + value);
        this.value = value;
    }

    public String value() {
        return value;
    }
}
