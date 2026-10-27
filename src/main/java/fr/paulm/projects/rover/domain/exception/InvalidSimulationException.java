package fr.paulm.projects.rover.domain.exception;

// Le contenu est bien formé, mais il ne respecte pas une règle de la simulation.
public class InvalidSimulationException extends InvalidInputException {

    public InvalidSimulationException(String message) {
        super(message);
    }
}
