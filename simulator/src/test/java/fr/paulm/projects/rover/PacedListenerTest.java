package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.Turn;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacedListenerTest {

    // A clock that only moves when the listener sleeps
    private Instant now = Instant.EPOCH;
    private final List<Duration> sleeps = new ArrayList<>();
    private final List<Turn> passed = new ArrayList<>();

    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    private PacedListener paced(double speed) {
        return new PacedListener(passed::add, speed, clock, duration -> {
            sleeps.add(duration);
            now = now.plus(duration);
        });
    }

    private Turn at(Duration time) {
        return new Turn(1, List.of(new Rover(new Position(0, 0), Direction.N)), Set.of(), time, OptionalInt.of(0));
    }

    @Test
    void eachTurnWaitsForItsTimeDividedByTheSpeed() {
        PacedListener listener = paced(60);
        listener.onTurn(at(Duration.ofMinutes(1)));
        listener.onTurn(at(Duration.ofMinutes(3)));
        // One minute of mission is one second at 60: the second turn comes 3 seconds after the start
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeps);
        assertEquals(2, passed.size());
    }

    @Test
    void aTurnThatIsLateDoesNotWait() {
        PacedListener listener = paced(60);
        now = now.plusSeconds(10);
        listener.onTurn(at(Duration.ofMinutes(1)));
        assertEquals(List.of(), sleeps);
        assertEquals(1, passed.size());
    }
}
