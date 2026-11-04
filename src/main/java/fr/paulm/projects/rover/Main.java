package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.MissionResult;
import fr.paulm.projects.rover.domain.ResultTracker;
import fr.paulm.projects.rover.domain.SequentialSimulation;
import fr.paulm.projects.rover.domain.SimultaneousSimulation;
import fr.paulm.projects.rover.domain.Simulation;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.StepByStepSimulation;
import fr.paulm.projects.rover.domain.Turn;
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
import java.util.stream.Collectors;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String STEP_BY_STEP_OPTION = "--step-by-step";
    private static final String SIMULTANEOUS_OPTION = "--simultaneous";
    private static final String HISTORY_OPTION = "--history";
    private static final String USAGE = "Usage: java -jar rover.jar [" + STEP_BY_STEP_OPTION + " | " + SIMULTANEOUS_OPTION
            + "] [" + HISTORY_OPTION + "] input.txt";

    // What the options ask for: the simulation mode, and whether to print the state of the rovers after each turn.
    private record Options(Simulation simulation, boolean history) {
    }

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
    // Arguments: the options, then the input file.
    int run(String[] args) {
        Optional<Options> options = optionsFor(args);
        if (options.isEmpty()) {
            LOGGER.error(USAGE);
            return 1;
        }
        String file = args[args.length - 1];
        try {
            SimulationReader reader = readerFactory.apply(Path.of(file));
            SimulationInput input = reader.read();
            // The turns are read once: each one is printed (with --history) as soon as it is computed,
            // and recorded to build the final results
            ResultTracker tracker = new ResultTracker(input);
            options.get().simulation().turns(input).forEach(turn -> {
                if (options.get().history()) {
                    displayTurn(turn);
                }
                tracker.record(turn);
            });
            display(tracker.results());
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

    // The options given before the file name. The simulation is sequential unless another mode is given.
    // Empty when the arguments are not valid: no file name, an unknown or repeated option, or two modes.
    private Optional<Options> optionsFor(String[] args) {
        if (args.length == 0 || args[args.length - 1].startsWith("--")) {
            return Optional.empty();
        }
        Simulation simulation = null;
        boolean history = false;
        for (int i = 0; i < args.length - 1; i++) {
            String option = args[i];
            if (option.equals(HISTORY_OPTION) && !history) {
                history = true;
            } else if (option.equals(STEP_BY_STEP_OPTION) && simulation == null) {
                simulation = new StepByStepSimulation();
            } else if (option.equals(SIMULTANEOUS_OPTION) && simulation == null) {
                simulation = new SimultaneousSimulation();
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new Options(simulation == null ? new SequentialSimulation() : simulation, history));
    }

    // One line per turn: "Turn 3: 0 2 N | 0 3 S (blocked: 1)". Rovers are numbered from 1, as in the warnings.
    private void displayTurn(Turn turn) {
        String rovers = turn.rovers().stream().map(Object::toString).collect(Collectors.joining(" | "));
        String blocked = turn.blockedRovers().stream().sorted()
                .map(i -> String.valueOf(i + 1)).collect(Collectors.joining(", "));
        System.out.println("Turn " + turn.number() + ": " + rovers + (blocked.isEmpty() ? "" : " (blocked: " + blocked + ")"));
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
