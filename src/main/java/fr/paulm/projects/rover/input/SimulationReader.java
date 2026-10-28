package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.SimulationInput;

// Une entrée possible de la simulation (fichier texte, et plus tard JSON, base de données…).
public interface SimulationReader {

    SimulationInput read();
}
