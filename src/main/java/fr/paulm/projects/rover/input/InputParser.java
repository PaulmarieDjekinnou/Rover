package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Instruction;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.RoverInstructions;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.input.exception.InputFormatException;
import fr.paulm.projects.rover.input.exception.InvalidDirectionException;
import fr.paulm.projects.rover.input.exception.UnknownInstructionException;

import java.util.ArrayList;
import java.util.List;

public final class InputParser {

    // Lit et valide tout le fichier : une erreur est levée avant qu'aucun rover ne bouge.
    public SimulationInput parse(List<String> rawLines) {
        List<String> lines = nonBlankLines(rawLines);
        Plateau plateau = parsePlateau(lines.get(0));
        // Après la ligne du plateau, chaque rover occupe deux lignes : position puis instructions.
        int roverCount = (lines.size() - 1) / 2;
        List<RoverInstructions> rovers = new ArrayList<>();
        for (int r = 0; r < roverCount; r++) {
            Rover rover = parseRover(lines.get(1 + 2 * r));
            List<Instruction> instructions = parseInstructions(lines.get(2 + 2 * r));
            rovers.add(new RoverInstructions(rover, instructions));
        }
        return new SimulationInput(plateau, rovers);
    }

    // Retire les espaces autour de chaque ligne, supprime les lignes vides et vérifie
    // qu'il y a une ligne de plateau suivie de paires (position, instructions) complètes.
    private List<String> nonBlankLines(List<String> rawLines) {
        List<String> lines = rawLines.stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .toList();
        if (lines.isEmpty()) {
            throw new InputFormatException("Fichier vide");
        }
        if ((lines.size() - 1) % 2 != 0) {
            throw new InputFormatException("Chaque rover doit avoir une ligne de position et une ligne d'instructions");
        }
        return lines;
    }

    private Plateau parsePlateau(String line) {
        String[] size = line.split("\\s+");
        if (size.length != 2) {
            throw new InputFormatException("Taille de plateau invalide : " + line);
        }
        try {
            return new Plateau(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        } catch (NumberFormatException e) {
            throw new InputFormatException("Taille de plateau invalide : " + line);
        }
    }

    private Rover parseRover(String line) {
        String[] pos = line.split("\\s+");
        if (pos.length != 3) {
            throw new InputFormatException("Position invalide : " + line);
        }
        int x;
        int y;
        try {
            x = Integer.parseInt(pos[0]);
            y = Integer.parseInt(pos[1]);
        } catch (NumberFormatException e) {
            throw new InputFormatException("Position invalide : " + line);
        }
        return new Rover(x, y, parseDirection(pos[2]));
    }

    private Direction parseDirection(String value) {
        try {
            return Direction.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidDirectionException(value);
        }
    }

    private List<Instruction> parseInstructions(String line) {
        List<Instruction> instructions = new ArrayList<>();
        for (char c : line.toCharArray()) {
            try {
                instructions.add(Instruction.valueOf(String.valueOf(c)));
            } catch (IllegalArgumentException e) {
                throw new UnknownInstructionException(c);
            }
        }
        return instructions;
    }
}
