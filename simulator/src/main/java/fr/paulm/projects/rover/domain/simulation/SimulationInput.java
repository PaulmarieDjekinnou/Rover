package fr.paulm.projects.rover.domain.simulation;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Surface;
import fr.paulm.projects.rover.domain.Terrain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The surface (the plateau, or the grid of a planet), its terrain, whether a cell holds one rover at most,
// and the rover missions to simulate, in input order.
// On a plateau, a cell holds one rover: rovers block each other. On a planet, a cell is about 15 km wide:
// several rovers can be in the same cell, kilometers apart, and never block each other.
// The mission list is copied and cannot be modified, so the checks done on construction (each rover
// starts on the surface, on its own cell when cells hold one rover) stay true for the lifetime of the object.
public record SimulationInput(Surface surface, Terrain terrain, boolean oneRoverPerCell, List<RoverMission> missions) {

    // Missions where a cell holds one rover at most.
    public SimulationInput(Surface surface, Terrain terrain, List<RoverMission> missions) {
        this(surface, terrain, true, missions);
    }

    // Missions on flat ground, where only the other rovers can block a move.
    public SimulationInput(Surface surface, List<RoverMission> missions) {
        this(surface, Terrain.FLAT, missions);
    }

    public SimulationInput {
        missions = List.copyOf(missions);
        Set<Position> startCells = new HashSet<>();
        for (RoverMission mission : missions) {
            Rover rover = mission.rover();
            if (!surface.contains(rover.position())) {
                throw new InvalidInputException("Rover starts outside the plateau: \"" + rover.position() + "\"");
            }
            if (!startCells.add(rover.position()) && oneRoverPerCell) {
                throw new InvalidInputException("Two rovers start on the same cell: \"" + rover.position() + "\"");
            }
        }
    }
}
