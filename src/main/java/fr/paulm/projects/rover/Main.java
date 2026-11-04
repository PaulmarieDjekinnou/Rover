package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.simulation.ClockSimulation;
import fr.paulm.projects.rover.domain.simulation.MissionResult;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.domain.simulation.TurnListener;
import fr.paulm.projects.rover.input.Scenario;
import fr.paulm.projects.rover.input.SimulationReader;
import fr.paulm.projects.rover.input.TextFileReader;
import fr.paulm.projects.rover.mars.mission.Batteries;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.PlanetMission;
import fr.paulm.projects.rover.mars.mission.RoverPositions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
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

    // Creates the reader of the input file. Replaceable in tests, to simulate an unexpected error.
    private final Function<Path, SimulationReader> readerFactory;
    // Where the history and the results are shown: rovers on a plateau, or a mission to a planet.
    // Tests give them a buffer instead of the standard output.
    private final ConsoleReport plateauReport;
    private final MissionReport missionReport;

    Main() {
        this(TextFileReader::new, System.out);
    }

    Main(Function<Path, SimulationReader> readerFactory, PrintStream out) {
        this.readerFactory = readerFactory;
        this.plateauReport = new ConsoleReport(out);
        this.missionReport = new MissionReport(out);
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
            switch (reader.read()) {
                case Scenario.OnPlateau(SimulationInput input) -> runOnPlateau(input, commandLine.get());
                case Scenario.OnPlanet(PlanetMission mission) -> runOnPlanet(mission, commandLine.get());
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

    private void runOnPlateau(SimulationInput input, CommandLine commandLine) {
        if (commandLine.map()) {
            LOGGER.warn("The {} option only applies to missions to a planet: ignored.", CommandLine.MAP_OPTION);
        }
        // Each turn is printed as soon as it is over, before the final positions
        TurnListener listener = commandLine.history() ? plateauReport::turn : TurnListener.NONE;
        plateauReport.results(commandLine.mode().simulation().run(input, listener));
    }

    // The rovers land first; those that landed then carry out their orders, all at the same time, on a common clock.
    private void runOnPlanet(PlanetMission mission, CommandLine commandLine) {
        commandLine.mode().option().ifPresent(option -> LOGGER.warn(
                "The {} option only applies to rovers on a plateau: a mission runs on a common clock.", option));
        Deployment deployment = mission.land();
        missionReport.landings(deployment);
        // The batteries set the pace: a rover waits when it must recharge
        Batteries batteries = new Batteries(deployment);
        // The exact positions follow each turn, the tracks are drawn on the maps; each turn is printed
        // as soon as it is over, with the history option
        RoverPositions positions = new RoverPositions(deployment);
        RoverTracks tracks = new RoverTracks();
        TurnListener listener = turn -> {
            positions.onTurn(turn);
            tracks.onTurn(turn);
            if (commandLine.history()) {
                missionReport.turn(turn, deployment, positions, batteries);
            }
        };
        List<MissionResult> results = new ClockSimulation(batteries).run(deployment.input(), listener);
        missionReport.results(deployment, results, positions, batteries);
        if (commandLine.map()) {
            missionReport.maps(deployment, results, tracks);
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
