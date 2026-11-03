package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.MissionResult;
import fr.paulm.projects.rover.domain.SequentialSimulation;
import fr.paulm.projects.rover.domain.Simulation;
import fr.paulm.projects.rover.domain.StepByStepSimulation;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;
import fr.paulm.projects.rover.input.SimulationReader;
import fr.paulm.projects.rover.input.TextFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String STEP_BY_STEP_OPTION = "--step-by-step";

    public static void main(String[] args) {
        int exitCode = new Main().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    // Runs the program and returns its exit code (0 on success, 1 on any error) instead of exiting,
    // so that error cases can be tested.
    // Arguments: the input file, optionally preceded by --step-by-step to move the rovers turn by turn.
    int run(String[] args) {
        boolean stepByStep = args.length == 2 && args[0].equals(STEP_BY_STEP_OPTION);
        if (args.length != 1 && !stepByStep) {
            LOGGER.error("Usage: java -jar rover.jar [" + STEP_BY_STEP_OPTION + "] input.txt");
            return 1;
        }
        String file = args[args.length - 1];
        Simulation simulation = stepByStep ? new StepByStepSimulation() : new SequentialSimulation();
        try {
            SimulationReader reader = new TextFileReader(Path.of(file));
            List<MissionResult> results = simulation.run(reader.read());
            display(results);
            return 0;
        } catch (UncheckedIOException e) {
            LOGGER.error("Cannot read file \"{}\": {}", file, reasonOf(e.getCause()));
            return 1;
        } catch (InvalidPathException e) {
            LOGGER.error("Invalid file path: \"{}\". {}", e.getInput(), e.getReason());
            return 1;
        } catch (InvalidInputException e) {
            LOGGER.error("Invalid input: {}", e.getMessage());
            return 1;
        }
    }

    // The final positions are the result of the program: they go to the standard output.
    // A warning on the standard error output reports each rover that had moves blocked by another rover.
    private void display(List<MissionResult> results) {
        for (int i = 0; i < results.size(); i++) {
            MissionResult result = results.get(i);
            if (result.blockedMoves() > 0) {
                LOGGER.warn("Rover {}: {} move(s) ignored because the cell is occupied by another rover.",
                        i + 1, result.blockedMoves());
            }
            System.out.println(result.finalRover());
        }
    }

    // A short explanation of why the file could not be read.
    private String reasonOf(IOException e) {
        return switch (e) {
            case NoSuchFileException _ -> "file not found";
            case AccessDeniedException _ -> "permission denied";
            default -> e.getMessage();
        };
    }
}
