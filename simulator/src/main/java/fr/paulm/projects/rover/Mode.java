package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.simulation.SequentialSimulation;
import fr.paulm.projects.rover.domain.simulation.Simulation;
import fr.paulm.projects.rover.domain.simulation.SimultaneousSimulation;
import fr.paulm.projects.rover.domain.simulation.StepByStepSimulation;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

// The simulation modes, each with the command-line option that selects it.
// Adding a mode only takes a constant here: the usage message and the option are derived from it.
enum Mode {
    // The default mode, used when no mode option is given
    SEQUENTIAL(null, SequentialSimulation::new),
    STEP_BY_STEP("--step-by-step", StepByStepSimulation::new),
    SIMULTANEOUS("--simultaneous", SimultaneousSimulation::new);

    private final String option;
    private final Supplier<Simulation> factory;

    Mode(String option, Supplier<Simulation> factory) {
        this.option = option;
        this.factory = factory;
    }

    // The mode selected by this option, if any.
    static Optional<Mode> fromOption(String option) {
        return Arrays.stream(values())
                .filter(mode -> option.equals(mode.option))
                .findFirst();
    }

    // The option that selects this mode; empty for the default mode.
    Optional<String> option() {
        return Optional.ofNullable(option);
    }

    Simulation simulation() {
        return factory.get();
    }
}
