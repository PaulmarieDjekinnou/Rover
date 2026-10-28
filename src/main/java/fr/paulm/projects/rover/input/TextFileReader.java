package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.enums.Instruction;
import fr.paulm.projects.rover.domain.Plateau;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.RoverInstructions;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Lit une simulation depuis un fichier texte : une ligne pour le plateau,
// puis deux lignes par rover (position, instructions).
public final class TextFileReader implements SimulationReader {
    private final Path path;

    public TextFileReader(Path path) {
        this.path = path;
    }

    @Override
    public SimulationInput read() {
        String content;
        try {
            content = Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return parse(content);
    }

    // Lit et valide tout le contenu : une erreur est levée avant qu'aucun rover ne bouge.
    private SimulationInput parse(String content) {
        List<String> lines = nonBlankLines(content.lines().toList());
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
            throw new InvalidInputException("Fichier vide : la première ligne doit donner la taille du plateau, par exemple « 5 5 »");
        }
        if ((lines.size() - 1) % 2 != 0) {
            throw new InvalidInputException("Chaque rover doit avoir une ligne de position et une ligne d'instructions");
        }
        return lines;
    }

    private Plateau parsePlateau(String line) {
        String[] size = line.split("\\s+");
        if (size.length != 2) {
            throw new InvalidInputException("Taille de plateau invalide : « " + line + " ». Attendu : deux entiers, par exemple « 5 5 »");
        }
        try {
            return new Plateau(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Taille de plateau invalide : « " + line + " ». Attendu : deux entiers, par exemple « 5 5 »");
        }
    }

    private Rover parseRover(String line) {
        String[] pos = line.split("\\s+");
        if (pos.length != 3) {
            throw new InvalidInputException("Position invalide : « " + line + " ». Attendu : x y direction, par exemple « 1 2 N »");
        }
        int x;
        int y;
        try {
            x = Integer.parseInt(pos[0]);
            y = Integer.parseInt(pos[1]);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Position invalide : « " + line + " ». Attendu : x y direction, par exemple « 1 2 N »");
        }
        return new Rover(x, y, parseDirection(pos[2]));
    }

    private Direction parseDirection(String value) {
        try {
            return Direction.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Direction invalide : « " + value + " ». Attendu : N, E, S ou W");
        }
    }

    private List<Instruction> parseInstructions(String line) {
        List<Instruction> instructions = new ArrayList<>();
        for (char c : line.toCharArray()) {
            try {
                instructions.add(Instruction.valueOf(String.valueOf(c)));
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("Instruction inconnue : « " + c + " ». Attendu : L, R ou M");
            }
        }
        return instructions;
    }
}
