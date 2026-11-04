package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.simulation.Simulation;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.domain.simulation.TurnListener;
import fr.paulm.projects.rover.input.Scenario;
import fr.paulm.projects.rover.input.SimulationReader;
import fr.paulm.projects.rover.input.TextFileReader;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.MarsMission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    // Creates the reader of the input file. Replaceable in tests, to simulate an unexpected error.
    private final Function<Path, SimulationReader> readerFactory;
    // Where the history and the results are shown. Tests give it a buffer instead of the standard output.
    private final ConsoleReport report;

    Main() {
        this(TextFileReader::new, new ConsoleReport(System.out));
    }

    Main(Function<Path, SimulationReader> readerFactory, ConsoleReport report) {
        this.readerFactory = readerFactory;
        this.report = report;
    }

    public static void main(String[] args) {
        int exitCode = new Main().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    // Runs the program and returns its exit code (0 on success, 1 on any error) instead of exiting,
    // so that error cases can be tested.
    // Arguments: the options, then the input file (see CommandLine).
    int run(String[] args) {
        Optional<CommandLine> commandLine = CommandLine.parse(args);
        if (commandLine.isEmpty()) {
            LOGGER.error(CommandLine.usage());
            return 1;
        }
        String file = commandLine.get().file();
        try {
            SimulationReader reader = readerFactory.apply(Path.of(file));
            Simulation simulation = commandLine.get().mode().simulation();
            boolean history = commandLine.get().history();
            switch (reader.read()) {
                case Scenario.OnPlateau(SimulationInput input) -> {
                    // Each turn is printed as soon as it is over, before the final positions
                    TurnListener listener = history ? report::turn : TurnListener.NONE;
                    report.results(simulation.run(input, listener));
                }
                case Scenario.OnMars(MarsMission mission) -> {
                    Deployment deployment = mission.land();
                    report.landings(deployment);
                    TurnListener listener = history ? turn -> report.turn(turn, deployment) : TurnListener.NONE;
                    report.results(deployment, simulation.run(deployment.input(), listener));
                }
            }
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

    // A short explanation of why the file could not be read.
    private String reasonOf(IOException e) {
        return switch (e) {
            case NoSuchFileException _ -> "file not found";
            case AccessDeniedException _ -> "permission denied";
            default -> e.getMessage();
        };
    }
}
