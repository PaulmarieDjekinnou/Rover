package fr.paulm.projects.rover.input.exception;

import fr.paulm.projects.rover.domain.exception.InvalidInputException;

// Le contenu de l'entrée ne respecte pas le format attendu.
public class InputFormatException extends InvalidInputException {

    public InputFormatException(String message) {
        super(message);
    }
}
