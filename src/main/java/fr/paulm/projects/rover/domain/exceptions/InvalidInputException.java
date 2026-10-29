package fr.paulm.projects.rover.domain.exceptions;

// The input content is invalid (format or simulation rule).
// It is the only exception Main reports as invalid input: its message explains the error to the user.
public final class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
