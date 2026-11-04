package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.MissionResult;
import fr.paulm.projects.rover.domain.SequentialSimulation;
import fr.paulm.projects.rover.domain.SimultaneousSimulation;
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
import java.util.Optional;
import java.util.function.Function;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String STEP_BY_STEP_OPTION = "--step-by-step";
    private static final String SIMULTANEOUS_OPTION = "--simultaneous";
    private static final String USAGE =
            "Usage: java -jar rover.jar [" + STEP_BY_STEP_OPTION + " | " + SIMULTANEOUS_OPTION + "] input.txt";

    // Creates the reader of the input file. Replaceable in tests, to simulate an unexpected error.
    private final Function<Path, SimulationReader> readerFactory;

    Main() {
        this(TextFileReader::new);
    }

    Main(Function<Path, SimulationReader> readerFactory) {
        this.readerFactory = readerFactory;
    }

    public static void main(String[] args) {
        int exitCode = new Main().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    // Runs the program and returns its exit code (0 on success, 1 on any error) instead of exiting,
    // so that error cases can be tested.
    // Arguments: the input file, optionally preceded by the simulation mode.
    int run(String[] args) {
        Optional<Simulation> simulation = simulationFor(args);
        if (simulation.isEmpty()) {
            LOGGER.error(USAGE);
            return 1;
        }
        String file = args[args.length - 1];
        try {
            SimulationReader reader = readerFactory.apply(Path.of(file));
            List<MissionResult> results = simulation.get().run(reader.read());
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
        } catch (RuntimeException e) {
            // A bug: a short message, followed by the stack trace for whoever fixes it
            LOGGER.error("Unexpected error: {}", e.toString(), e);
            return 1;
        }
    }

    // The simulation mode: sequential by default, or the one given by the option before the file name.
    // Empty when the arguments are not valid, including an option without a file name.
    private Optional<Simulation> simulationFor(String[] args) {
        if (args.length == 1 && !args[0].startsWith("--")) {
            return Optional.of(new SequentialSimulation());
        }
        if (args.length != 2) {
            return Optional.empty();
        }
        return switch (args[0]) {
            case STEP_BY_STEP_OPTION -> Optional.of(new StepByStepSimulation());
            case SIMULTANEOUS_OPTION -> Optional.of(new SimultaneousSimulation());
            default -> Optional.empty();
        };
    }

    // The final positions are the result of the program: they go to the standard output.
    // A warning on the standard error output reports each rover that had moves blocked by another rover.
    private void display(List<MissionResult> results) {
        for (int i = 0; i < results.size(); i++) {
            MissionResult result = results.get(i);
            if (result.blockedMoves() > 0) {
                LOGGER.warn("Rover {}: {} move(s) blocked by another rover.", i + 1, result.blockedMoves());
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
