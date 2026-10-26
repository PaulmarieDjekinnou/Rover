package fr.paulm.projects;

import java.util.ArrayList;
import java.util.List;

final class Mission {
    private final List<String> rawLines;
    private final List<Rover> rovers = new ArrayList<>();

    Mission(List<String> rawLines) {
        this.rawLines = List.copyOf(rawLines);
    }

    // Déplace chaque rover selon ses instructions, dans l'ordre du fichier.
    void run() {
        List<String> lines = nonBlankLines(rawLines);
        Plateau plateau = parsePlateau(lines.get(0));
        rovers.clear();
        // Après la ligne du plateau, chaque rover occupe deux lignes : position puis instructions.
        int roverCount = (lines.size() - 1) / 2;
        for (int r = 0; r < roverCount; r++) {
            String position = lines.get(1 + 2 * r);
            String instructions = lines.get(2 + 2 * r);
            Rover rover = parseRover(plateau, position);
            rover.execute(instructions);
            rovers.add(rover);
        }
    }

    // Affiche la position finale de chaque rover, une par ligne.
    void display() {
        rovers.forEach(System.out::println);
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

    private Rover parseRover(Plateau plateau, String line) {
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
        return new Rover(plateau, x, y, parseDirection(pos[2]));
    }

    private Direction parseDirection(String value) {
        try {
            return Direction.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Direction invalide : " + value);
        }
    }
}
