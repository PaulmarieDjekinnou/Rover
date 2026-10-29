package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;

import java.util.List;

// The plateau and the rover missions to simulate, in input order.
// The mission list is copied and cannot be modified, so the check that each rover starts
// on the plateau, done on construction, stays true for the lifetime of the object.
public record SimulationInput(Plateau plateau, List<RoverMission> missions) {

    public SimulationInput {
        missions = List.copyOf(missions);
        for (RoverMission mission : missions) {
            Rover rover = mission.rover();
            if (!plateau.contains(rover.x(), rover.y())) {
                throw new InvalidInputException("Rover starts outside the plateau: \"" + rover.x() + " " + rover.y() + "\"");
            }
        }
    }
}
