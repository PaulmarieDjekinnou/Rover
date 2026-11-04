package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.RoverMission;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// The text format: one line for the plateau, then two lines per rover (position, instructions).
// Does not know where the text comes from.
public final class TextFormatParser {

    // The shape of a position line ("1 2 N"). Instructions never contain digits, so a line with this shape
    // where instructions are expected means that the rover above has no instruction line.
    private static final Pattern POSITION_SHAPE = Pattern.compile("-?\\d+\\s+-?\\d+\\s+\\S+");

    // A non-blank line of the content, trimmed, with its line number in the original content (from 1),
    // so that error messages can point to the right line even when blank lines were skipped.
    private record Line(int number, String text) {

        InvalidInputException error(String message) {
            return new InvalidInputException("Line " + number + ": " + message);
        }
    }

    // Reads and validates the whole content: an error is thrown before any rover moves.
    public SimulationInput parse(String content) {
        List<Line> lines = nonBlankLines(content);
        Plateau plateau = parsePlateau(lines.get(0));
        // After the plateau line, each rover takes two lines: position, then instructions.
        List<RoverMission> missions = new ArrayList<>();
        for (int i = 1; i < lines.size(); i += 2) {
            Line positionLine = lines.get(i);
            if (i + 1 == lines.size() || looksLikePosition(lines.get(i + 1))) {
                throw positionLine.error("Each rover needs a position line and an instruction line");
            }
            missions.add(new RoverMission(parseRover(positionLine), parseInstructions(lines.get(i + 1))));
        }
        return new SimulationInput(plateau, missions);
    }

    // Trims each line, removes blank lines and checks that there is a plateau line.
    private List<Line> nonBlankLines(String content) {
        List<String> rawLines = content.lines().toList();
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < rawLines.size(); i++) {
            String text = rawLines.get(i).trim();
            if (!text.isEmpty()) {
                lines.add(new Line(i + 1, text));
            }
        }
        if (lines.isEmpty()) {
            throw new InvalidInputException("Empty file: the first line must give the plateau size, for example \"5 5\"");
        }
        return lines;
    }

    private boolean looksLikePosition(Line line) {
        return POSITION_SHAPE.matcher(line.text()).matches();
    }

    private Plateau parsePlateau(Line line) {
        String[] size = line.text().split("\\s+");
        String expected = "Invalid plateau size: \"" + line.text() + "\". Expected: two integers, for example \"5 5\"";
        if (size.length != 2) {
            throw line.error(expected);
        }
        try {
            return new Plateau(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        } catch (NumberFormatException e) {
            throw line.error(expected);
        } catch (InvalidInputException e) {
            // A rule of the Plateau itself (negative size): add the line number to its message
            throw line.error(e.getMessage());
        }
    }

    private Rover parseRover(Line line) {
        String[] pos = line.text().split("\\s+");
        String expected = "Invalid position: \"" + line.text() + "\". Expected: x y direction, for example \"1 2 N\"";
        if (pos.length != 3) {
            throw line.error(expected);
        }
        int x;
        int y;
        try {
            x = Integer.parseInt(pos[0]);
            y = Integer.parseInt(pos[1]);
        } catch (NumberFormatException e) {
            throw line.error(expected);
        }
        return new Rover(new Position(x, y), parseDirection(line, pos[2]));
    }

    private Direction parseDirection(Line line, String value) {
        return Direction.fromCode(value)
                .orElseThrow(() -> line.error("Invalid direction: \"" + value + "\". Expected: N, E, S or W"));
    }

    private List<Instruction> parseInstructions(Line line) {
        List<Instruction> instructions = new ArrayList<>();
        for (char c : line.text().toCharArray()) {
            instructions.add(Instruction.fromCode(c)
                    .orElseThrow(() -> line.error("Unknown instruction: \"" + c + "\". Expected: L, R, M or B")));
        }
        return instructions;
    }
}
