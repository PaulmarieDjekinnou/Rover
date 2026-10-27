package fr.paulm.projects.rover.domain.exception;

// Toute erreur due au contenu de l'entrée : c'est la seule exception que Main présente comme une entrée invalide.
public abstract class InvalidInputException extends RuntimeException {

    protected InvalidInputException(String message) {
        super(message);
    }
}
