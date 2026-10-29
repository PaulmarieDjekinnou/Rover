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
        ExitCode exitCode = new Main().run(args);
        if (exitCode != ExitCode.SUCCESS) {
            System.exit(exitCode.value());
        }
    }

    // Runs the program and returns its exit code instead of exiting, so that error cases can be tested.
    ExitCode run(String[] args) {
        if (args.length != 1) {
            LOGGER.error("Usage: java -jar rover.jar input.txt");
            return ExitCode.USAGE_ERROR;
        }
        try {
            SimulationReader reader = new TextFileReader(Path.of(args[0]));
            new Simulation(reader.read()).run();
            return ExitCode.SUCCESS;
        } catch (UncheckedIOException e) {
            LOGGER.error("Cannot read file: {}", e.getCause().getMessage());
            return ExitCode.UNREADABLE_FILE;
        } catch (InvalidPathException e) {
            LOGGER.error("Cannot read file: {}", e.getMessage());
            return ExitCode.UNREADABLE_FILE;
        } catch (InvalidInputException e) {
            LOGGER.error("Invalid input: {}", e.getMessage());
            return ExitCode.INVALID_INPUT;
        }
    }
}
