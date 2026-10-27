package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Simulation;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.exception.InvalidInputException;
import fr.paulm.projects.rover.input.InputParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Utilisation : java -jar rover.jar input.txt");
            System.exit(1);
        }
        try {
            SimulationInput input = new InputParser().parse(Files.readAllLines(Path.of(args[0])));
            new Simulation(input).run();
        } catch (IOException | InvalidPathException e) {
            System.err.println("Impossible de lire le fichier : " + e.getMessage());
            System.exit(1);
        } catch (InvalidInputException e) {
            System.err.println("Entrée invalide : " + e.getMessage());
            System.exit(1);
        }
    }
}
