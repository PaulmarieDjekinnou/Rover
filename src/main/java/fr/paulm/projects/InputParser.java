package fr.paulm.projects;

import java.util.ArrayList;
import java.util.List;

final class InputParser {

    // Lit et valide tout le fichier : une erreur est levée avant qu'aucun rover ne bouge.
    SimulationInput parse(List<String> rawLines) {
        List<String> lines = nonBlankLines(rawLines);
        Plateau plateau = parsePlateau(lines.get(0));
        // Après la ligne du plateau, chaque rover occupe deux lignes : position puis instructions.
        int roverCount = (lines.size() - 1) / 2;
        List<RoverInstructions> rovers = new ArrayList<>();
        for (int r = 0; r < roverCount; r++) {
            Rover rover = parseRover(lines.get(1 + 2 * r));
            String instructions = parseInstructions(lines.get(2 + 2 * r));
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
            throw new IllegalArgumentException("Fichier vide");
        }
        if ((lines.size() - 1) % 2 != 0) {
            throw new IllegalArgumentException("Chaque rover doit avoir une ligne de position et une ligne d'instructions");
        }
        return lines;
    }

    private Plateau parsePlateau(String line) {
        String[] size = line.split("\\s+");
        if (size.length != 2) {
            throw new IllegalArgumentException("Taille de plateau invalide : " + line);
        }
        try {
            return new Plateau(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Taille de plateau invalide : " + line);
        }
    }

    private Rover parseRover(String line) {
        String[] pos = line.split("\\s+");
        if (pos.length != 3) {
            throw new IllegalArgumentException("Position invalide : " + line);
        }
        int x;
        int y;
        try {
            x = Integer.parseInt(pos[0]);
            y = Integer.parseInt(pos[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Position invalide : " + line);
        }
        return new Rover(x, y, parseDirection(pos[2]));
    }

    private Direction parseDirection(String value) {
        try {
            return Direction.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Direction invalide : " + value);
        }
    }

    private String parseInstructions(String line) {
        for (char c : line.toCharArray()) {
            if (c != 'L' && c != 'R' && c != 'M') {
                throw new IllegalArgumentException("Instruction inconnue : " + c);
            }
        }
        return line;
    }
}
