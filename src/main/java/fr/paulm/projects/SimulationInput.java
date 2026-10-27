package fr.paulm.projects;

import java.util.List;

// Le plateau et les rovers à simuler, dans l'ordre du fichier.
// Vérifie à la construction que chaque rover démarre sur le plateau.
record SimulationInput(Plateau plateau, List<RoverInstructions> rovers) {

    SimulationInput {
        for (RoverInstructions r : rovers) {
            Rover rover = r.rover();
            if (!plateau.contains(rover.x(), rover.y())) {
                throw new IllegalArgumentException("Le rover démarre hors du plateau : " + rover.x() + " " + rover.y());
            }
        }
    }
}
