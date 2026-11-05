package fr.paulm.projects.rover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineTest {

    // "--history input.txt" → ["--history", "input.txt"]
    private static Optional<CommandLine> parse(String args) {
        return CommandLine.parse(args.split(" "));
    }

    @Test
    void fileAloneRunsTheSequentialModeWithoutHistory() {
        assertEquals(Optional.of(new CommandLine(Mode.SEQUENTIAL, false, false, "input.txt")), parse("input.txt"));
    }

    @ParameterizedTest
    @CsvSource({"--step-by-step, STEP_BY_STEP", "--simultaneous, SIMULTANEOUS"})
    void modeOptionSelectsTheMode(String option, Mode mode) {
        assertEquals(Optional.of(new CommandLine(mode, false, false, "input.txt")), parse(option + " input.txt"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--history --simultaneous input.txt", "--simultaneous --history input.txt"})
    void historyAndModeCanBeGivenInAnyOrder(String args) {
        assertEquals(Optional.of(new CommandLine(Mode.SIMULTANEOUS, true, false, "input.txt")), parse(args));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "--history",                                  // no file name
            "--simultaneous",
            "a.txt b.txt",                                // two files
            "--fast input.txt",                           // unknown option
            "--history --history input.txt",              // repeated option
            "--map --map input.txt",
            "--telemetry input.txt",                      // no server
            "--telemetry --map input.txt",
            "--telemetry a:1 --telemetry b:2 input.txt",
            "--step-by-step --step-by-step input.txt",
            "--step-by-step --simultaneous input.txt",    // two modes
            "input.txt --history"})                       // option after the file name
    void invalidArgumentsAreRejected(String args) {
        assertEquals(Optional.empty(), parse(args));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--map --history --step-by-step input.txt", "--step-by-step --history --map input.txt"})
    void mapCanBeGivenWithTheOtherOptionsInAnyOrder(String args) {
        assertEquals(Optional.of(new CommandLine(Mode.STEP_BY_STEP, true, true, "input.txt")), parse(args));
    }

    @Test
    void telemetryOptionTakesTheKafkaServer() {
        assertEquals(Optional.of(new CommandLine(Mode.SEQUENTIAL, false, true, Optional.of("localhost:9092"), "mission.txt")),
                parse("--telemetry localhost:9092 --map mission.txt"));
    }

    @Test
    void noArgumentIsRejected() {
        assertEquals(Optional.empty(), CommandLine.parse(new String[]{}));
    }

    @Test
    void usageListsEveryModeOption() {
        assertEquals("Usage: java -jar rover.jar [--step-by-step | --simultaneous] [--history] [--map] [--telemetry server:port | file:path] input.txt", CommandLine.usage());
    }
}
