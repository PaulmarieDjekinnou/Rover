package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.exception.RoverOutsidePlateauException;

import java.util.List;

// Le plateau et les rovers à simuler, dans l'ordre du fichier.
// Vérifie à la construction que chaque rover démarre sur le plateau.
public record SimulationInput(Plateau plateau, List<RoverInstructions> rovers) {

    public SimulationInput {
        for (RoverInstructions r : rovers) {
            Rover rover = r.rover();
            if (!plateau.contains(rover.x(), rover.y())) {
                throw new RoverOutsidePlateauException(rover.x(), rover.y());
            }
        }
    }
}
