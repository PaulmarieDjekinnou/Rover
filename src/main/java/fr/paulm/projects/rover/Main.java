package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Simulation;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;
import fr.paulm.projects.rover.input.SimulationReader;
import fr.paulm.projects.rover.input.TextFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        int exitCode = new Main().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    // Runs the program and returns its exit code (0 on success, 1 on any error) instead of exiting,
    // so that error cases can be tested.
    int run(String[] args) {
        if (args.length != 1) {
            LOGGER.error("Usage: java -jar rover.jar input.txt");
            return 1;
        }
        try {
            SimulationReader reader = new TextFileReader(Path.of(args[0]));
            new Simulation(reader.read()).run();
            return 0;
        } catch (UncheckedIOException e) {
            LOGGER.error("Cannot read file: {}", e.getCause().getMessage());
            return 1;
        } catch (InvalidPathException e) {
            LOGGER.error("Invalid file path: \"{}\". {}", e.getInput(), e.getReason());
            return 1;
        } catch (InvalidInputException e) {
            LOGGER.error("Invalid input: {}", e.getMessage());
            return 1;
        }
    }
}
