package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Simulation;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;
import fr.paulm.projects.rover.input.SimulationReader;
import fr.paulm.projects.rover.input.TextFileReader;

import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Utilisation : java -jar rover.jar input.txt");
            System.exit(1);
        }
        try {
            SimulationReader reader = new TextFileReader(Path.of(args[0]));
            new Simulation(reader.read()).run();
        } catch (UncheckedIOException e) {
            System.err.println("Impossible de lire le fichier : " + e.getCause().getMessage());
            System.exit(1);
        } catch (InvalidPathException e) {
            System.err.println("Impossible de lire le fichier : " + e.getMessage());
            System.exit(1);
        } catch (InvalidInputException e) {
            System.err.println("Entrée invalide : " + e.getMessage());
            System.exit(1);
        }
    }
}
