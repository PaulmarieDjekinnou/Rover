package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.enums.Instruction;

import java.util.List;

// A rover's mission: the rover ready to start and the instructions it must execute.
public record RoverMission(Rover rover, List<Instruction> instructions) {

    public RoverMission {
        instructions = List.copyOf(instructions);
    }
}
