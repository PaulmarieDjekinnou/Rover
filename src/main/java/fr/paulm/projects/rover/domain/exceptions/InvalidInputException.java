package fr.paulm.projects.rover.domain.exceptions;

// Le contenu de l'entrée est invalide (format ou règle de la simulation).
// C'est la seule exception que Main présente comme une entrée invalide : son message explique l'erreur à l'utilisateur.
public final class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
