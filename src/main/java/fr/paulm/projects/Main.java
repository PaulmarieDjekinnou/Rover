package fr.paulm.projects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Utilisation : java -jar rover.jar input.txt");
            System.exit(1);
        }
        try {
            Mission mission = new Mission(Files.readAllLines(Path.of(args[0])));
            mission.run();
            mission.display();
        } catch (IOException e) {
            System.err.println("Impossible de lire le fichier : " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Entrée invalide : " + e.getMessage());
            System.exit(1);
        }
    }
}
