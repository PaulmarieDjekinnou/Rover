package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.domain.simulation.TurnListener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// Slows the simulation down to a given speed, so that a mission can be followed live: before passing a turn on,
// it waits until the real time since its creation (the start of the mission) reaches the mission time divided by
// the speed (a speed of
// 8877 runs a sol in about 10 seconds). The clock computes its turns one by one and calls its listener at once,
// so waiting here slows the whole simulation, and the domain does not change.
final class PacedListener implements TurnListener {

    // How to wait: Thread.sleep in the program, nothing in tests
    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final TurnListener delegate;
    private final double speed;
    private final Clock clock;
    private final Sleeper sleeper;
    private final Instant start;

    PacedListener(TurnListener delegate, double speed, Clock clock, Sleeper sleeper) {
        this.delegate = delegate;
        this.speed = speed;
        this.clock = clock;
        this.sleeper = sleeper;
        this.start = clock.instant();
    }

    @Override
    public void onTurn(Turn turn) {
        Instant due = start.plusMillis(Math.round(turn.time().toMillis() / speed));
        Duration wait = Duration.between(clock.instant(), due);
        if (wait.isPositive()) {
            try {
                sleeper.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        delegate.onTurn(turn);
    }
}
