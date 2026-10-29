package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.RoverMission;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;

import java.util.ArrayList;
import java.util.List;

// The text format: one line for the plateau, then two lines per rover (position, instructions).
// Does not know where the text comes from.
public final class TextFormatParser {

    // Reads and validates the whole content: an error is thrown before any rover moves.
    public SimulationInput parse(String content) {
        List<String> lines = nonBlankLines(content.lines().toList());
        Plateau plateau = parsePlateau(lines.get(0));
        // After the plateau line, each rover takes two lines: position, then instructions.
        int roverCount = (lines.size() - 1) / 2;
        List<RoverMission> missions = new ArrayList<>();
        for (int r = 0; r < roverCount; r++) {
            Rover rover = parseRover(lines.get(1 + 2 * r));
            List<Instruction> instructions = parseInstructions(lines.get(2 + 2 * r));
            missions.add(new RoverMission(rover, instructions));
        }
        return new SimulationInput(plateau, missions);
    }

    // Trims each line, removes blank lines and checks that there is a plateau line
    // followed by complete (position, instructions) pairs.
    private List<String> nonBlankLines(List<String> rawLines) {
        List<String> lines = rawLines.stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .toList();
        if (lines.isEmpty()) {
            throw new InvalidInputException("Empty file: the first line must give the plateau size, for example \"5 5\"");
        }
        if ((lines.size() - 1) % 2 != 0) {
            throw new InvalidInputException("Each rover needs a position line and an instruction line");
        }
        return lines;
    }

    private Plateau parsePlateau(String line) {
        String[] size = line.split("\\s+");
        if (size.length != 2) {
            throw new InvalidInputException("Invalid plateau size: \"" + line + "\". Expected: two integers, for example \"5 5\"");
        }
        try {
            return new Plateau(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Invalid plateau size: \"" + line + "\". Expected: two integers, for example \"5 5\"");
        }
    }

    private Rover parseRover(String line) {
        String[] pos = line.split("\\s+");
        if (pos.length != 3) {
            throw new InvalidInputException("Invalid position: \"" + line + "\". Expected: x y direction, for example \"1 2 N\"");
        }
        int x;
        int y;
        try {
            x = Integer.parseInt(pos[0]);
            y = Integer.parseInt(pos[1]);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Invalid position: \"" + line + "\". Expected: x y direction, for example \"1 2 N\"");
        }
        return new Rover(x, y, parseDirection(pos[2]));
    }

    private Direction parseDirection(String value) {
        try {
            return Direction.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid direction: \"" + value + "\". Expected: N, E, S or W");
        }
    }

    private List<Instruction> parseInstructions(String line) {
        List<Instruction> instructions = new ArrayList<>();
        for (char c : line.toCharArray()) {
            instructions.add(parseInstruction(c));
        }
        return instructions;
    }

    // The letters of the text format: L, R and M.
    private Instruction parseInstruction(char c) {
        return switch (c) {
            case 'L' -> Instruction.LEFT;
            case 'R' -> Instruction.RIGHT;
            case 'M' -> Instruction.MOVE;
            default -> throw new InvalidInputException("Unknown instruction: \"" + c + "\". Expected: L, R or M");
        };
    }
}
