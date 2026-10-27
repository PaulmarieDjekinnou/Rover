package fr.paulm.projects.rover.domain;

import java.util.List;

// Un rover prêt à partir et la suite d'instructions qu'il doit exécuter.
public record RoverInstructions(Rover rover, List<Instruction> instructions) {
}
