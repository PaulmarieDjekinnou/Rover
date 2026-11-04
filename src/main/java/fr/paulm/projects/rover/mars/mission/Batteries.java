package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.Pace;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;
import fr.paulm.projects.rover.mars.rover.PowerSystem;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

// The batteries of the rovers that landed, followed through the mission: the pace of the rovers on the clock.
// Each battery starts full. It is charged by the power source of its rover, and drained by the rover: more when it
// drives or turns (PowerSystem.ACTIVE_WATTS) than when it waits (PowerSystem.IDLE_WATTS).
// A rover works while its battery holds more than a reserve (a fifth of its capacity). Below it, the rover waits
// and recharges until its battery is four fifths full, then goes on: a solar rover drives by day, and spends its
// nights waiting. A rover whose battery does not reach that level in a whole year of waiting is stopped for good,
// as Spirit was in 2010.
// The power is computed step by step (10 minutes), at the cell where the instruction starts.
public final class Batteries implements Pace {

    private static final Duration STEP = Duration.ofMinutes(10);
    private static final double RESERVE = 0.2;
    private static final double RECHARGED = 0.8;

    private final Planet planet;
    private final Sun sun;
    private final List<PowerSystem> systems;
    private final double[] levels;
    private final boolean[] charging;

    public Batteries(Deployment deployment) {
        this.planet = deployment.planet();
        this.sun = deployment.sun();
        this.systems = deployment.landed().stream().map(touchdown -> touchdown.plan().power()).toList();
        this.levels = systems.stream().mapToDouble(PowerSystem::batteryWattHours).toArray();
        this.charging = new boolean[systems.size()];
    }

    // How full the battery of a rover is after its last instruction, from 0 to 1.
    public double charge(int rover) {
        return levels[rover] / systems.get(rover).batteryWattHours();
    }

    @Override
    public Optional<Duration> duration(int rover, Rover state, Duration start, Duration busy) {
        PowerSystem system = systems.get(rover);
        double capacity = system.batteryWattHours();
        Coordinates point = planet.centerOf(state.position());
        Duration year = Duration.ofMillis(Math.round(planet.orbit().yearInSols() * planet.dayLength().toMillis()));
        Duration time = start;
        Duration left = busy;
        Duration waiting = Duration.ZERO;
        while (left.isPositive()) {
            if (charging[rover] && levels[rover] >= RECHARGED * capacity) {
                charging[rover] = false;
            } else if (!charging[rover] && levels[rover] <= RESERVE * capacity) {
                charging[rover] = true;
            }
            Duration step = charging[rover] || STEP.compareTo(left) < 0 ? STEP : left;
            double used = charging[rover] ? PowerSystem.IDLE_WATTS : PowerSystem.ACTIVE_WATTS;
            double produced = system.source().watts(sun, point, time);
            double hours = step.toMillis() / 3_600_000.0;
            levels[rover] = Math.clamp(levels[rover] + (produced - used) * hours, 0, capacity);
            if (charging[rover]) {
                waiting = waiting.plus(step);
                if (waiting.compareTo(year) > 0) {
                    return Optional.empty();
                }
            } else {
                left = left.minus(step);
                waiting = Duration.ZERO;
            }
            time = time.plus(step);
        }
        return Optional.of(time.minus(start));
    }
}
