package fr.paulm.projects.rover;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

// What the command line asks for: the simulation mode, whether to print the state of the rovers
// after each turn, whether to draw a map around each rover at the end of a mission, and the input file.
record CommandLine(Mode mode, boolean history, boolean map, String file) {

    static final String HISTORY_OPTION = "--history";
    static final String MAP_OPTION = "--map";

    // The options come before the file name, in any order. The mode is sequential unless another one is given.
    // Empty when the arguments are not valid: no file name, an unknown or repeated option, or two modes.
    static Optional<CommandLine> parse(String[] args) {
        if (args.length == 0 || args[args.length - 1].startsWith("--")) {
            return Optional.empty();
        }
        Mode mode = null;
        boolean history = false;
        boolean map = false;
        for (int i = 0; i < args.length - 1; i++) {
            String option = args[i];
            Optional<Mode> optionMode = Mode.fromOption(option);
            if (option.equals(HISTORY_OPTION) && !history) {
                history = true;
            } else if (option.equals(MAP_OPTION) && !map) {
                map = true;
            } else if (optionMode.isPresent() && mode == null) {
                mode = optionMode.get();
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new CommandLine(mode == null ? Mode.SEQUENTIAL : mode, history, map, args[args.length - 1]));
    }

    // Lists every mode option, so that a new mode appears in it without changing this method.
    static String usage() {
        String modes = Arrays.stream(Mode.values())
                .flatMap(mode -> mode.option().stream())
                .collect(Collectors.joining(" | "));
        return "Usage: java -jar rover.jar [" + modes + "] [" + HISTORY_OPTION + "] [" + MAP_OPTION + "] input.txt";
    }
}
