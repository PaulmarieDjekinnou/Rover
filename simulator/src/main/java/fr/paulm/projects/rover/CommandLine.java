package fr.paulm.projects.rover;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

// What the command line asks for: the simulation mode, whether to print the state of the rovers
// after each turn, whether to draw a map around each rover at the end of a mission, where to send the telemetry
// of a mission (a Kafka server or a file), how fast to run a mission (seconds of mission per real second, to follow
// it live), and the input file.
record CommandLine(Mode mode, boolean history, boolean map, Optional<String> telemetry, Optional<Double> speed,
                   String file) {

    static final String HISTORY_OPTION = "--history";
    static final String MAP_OPTION = "--map";
    static final String TELEMETRY_OPTION = "--telemetry";
    static final String SPEED_OPTION = "--speed";

    // At full speed.
    CommandLine(Mode mode, boolean history, boolean map, Optional<String> telemetry, String file) {
        this(mode, history, map, telemetry, Optional.empty(), file);
    }

    // Without telemetry, at full speed.
    CommandLine(Mode mode, boolean history, boolean map, String file) {
        this(mode, history, map, Optional.empty(), file);
    }

    // The options come before the file name, in any order; --telemetry is followed by its target, --speed by a
    // positive number.
    // The mode is sequential unless another one is given.
    // Empty when the arguments are not valid: no file name, an unknown or repeated option, or two modes.
    static Optional<CommandLine> parse(String[] args) {
        if (args.length == 0 || args[args.length - 1].startsWith("--")) {
            return Optional.empty();
        }
        Mode mode = null;
        boolean history = false;
        boolean map = false;
        String telemetry = null;
        Double speed = null;
        for (int i = 0; i < args.length - 1; i++) {
            String option = args[i];
            Optional<Mode> optionMode = Mode.fromOption(option);
            if (option.equals(HISTORY_OPTION) && !history) {
                history = true;
            } else if (option.equals(MAP_OPTION) && !map) {
                map = true;
            } else if (option.equals(TELEMETRY_OPTION) && telemetry == null && i + 2 < args.length
                    && !args[i + 1].startsWith("--")) {
                telemetry = args[++i];
            } else if (option.equals(SPEED_OPTION) && speed == null && i + 2 < args.length) {
                Optional<Double> value = positiveNumber(args[++i]);
                if (value.isEmpty()) {
                    return Optional.empty();
                }
                speed = value.get();
            } else if (optionMode.isPresent() && mode == null) {
                mode = optionMode.get();
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new CommandLine(mode == null ? Mode.SEQUENTIAL : mode, history, map,
                Optional.ofNullable(telemetry), Optional.ofNullable(speed), args[args.length - 1]));
    }

    private static Optional<Double> positiveNumber(String text) {
        try {
            double value = Double.parseDouble(text);
            return value > 0 && Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    // Lists every mode option, so that a new mode appears in it without changing this method.
    static String usage() {
        String modes = Arrays.stream(Mode.values())
                .flatMap(mode -> mode.option().stream())
                .collect(Collectors.joining(" | "));
        return "Usage: java -jar rover.jar [" + modes + "] [" + HISTORY_OPTION + "] [" + MAP_OPTION + "] ["
                + TELEMETRY_OPTION + " server:port | file:path] [" + SPEED_OPTION + " factor] input.txt";
    }
}
