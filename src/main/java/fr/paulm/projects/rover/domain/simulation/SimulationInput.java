package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The plateau and the rover missions to simulate, in input order.
// The mission list is copied and cannot be modified, so the checks done on construction (each rover
// starts on the plateau, on its own cell) stay true for the lifetime of the object.
public record SimulationInput(Plateau plateau, List<RoverMission> missions) {

    public SimulationInput {
        missions = List.copyOf(missions);
        Set<Position> startCells = new HashSet<>();
        for (RoverMission mission : missions) {
            Rover rover = mission.rover();
            if (!plateau.contains(rover.position())) {
                throw new InvalidInputException("Rover starts outside the plateau: \"" + rover.position() + "\"");
            }
            if (!startCells.add(rover.position())) {
                throw new InvalidInputException("Two rovers start on the same cell: \"" + rover.position() + "\"");
            }
        }
    }
}
