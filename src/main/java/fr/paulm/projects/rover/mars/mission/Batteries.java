package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.Pace;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;
import fr.paulm.projects.rover.mars.rover.PowerSystem;

import java.time.Duration;
import java.util.List;

// The batteries of the rovers that landed, followed through the mission: the pace of the rovers on the clock.
// Each battery starts full. It is charged by the power source of its rover, and drained by the rover: more when it
// drives or turns (PowerSystem.ACTIVE_WATTS) than when it waits (PowerSystem.IDLE_WATTS).
// A rover works while its battery holds more than a reserve (a fifth of its capacity). Below it, the rover waits
// and recharges until its battery is four fifths full, then goes on: a solar rover drives by day, and spends its
// nights waiting. A rover whose battery does not reach that level in a whole year of waiting is stopped for good,
// as Spirit was in 2010.
// The power is computed step by step (10 minutes), where the rover is: between the centers of the cell it leaves
// and of the cell it goes to, as far along as it has driven (across a pole, on one side for the first half of the
// way, on the other side for the second half).
public final class Batteries implements Pace {

    private static final Duration STEP = Duration.ofMinutes(10);
    private static final double RESERVE = 0.2;
    private static final double RECHARGED = 0.8;

    private final Planet planet;
    private final Sun sun;
    private final List<PowerSystem> systems;
    private final List<Duration> starts;
    private final double[] levels;
    private final boolean[] charging;

    public Batteries(Deployment deployment) {
        this.planet = deployment.planet();
        this.sun = deployment.sun();
        this.systems = deployment.landed().stream().map(touchdown -> touchdown.plan().power()).toList();
        this.starts = deployment.landed().stream().map(Touchdown::time).toList();
        this.levels = systems.stream().mapToDouble(PowerSystem::batteryWattHours).toArray();
        this.charging = new boolean[systems.size()];
    }

    // A rover starts when it lands.
    @Override
    public Duration start(int rover) {
        return starts.get(rover);
    }

    // How full the battery of a rover is after its last instruction, from 0 to 1.
    public double charge(int rover) {
        return levels[rover] / systems.get(rover).batteryWattHours();
    }

    @Override
    public Timing timing(int rover, Rover from, Rover to, Duration start, Duration busy) {
        PowerSystem system = systems.get(rover);
        double capacity = system.batteryWattHours();
        Duration year = Duration.ofMillis(Math.round(planet.orbit().yearInSols() * planet.dayLength().toMillis()));
        Duration time = start;
        Duration left = busy;
        // waiting: since the rover last made progress; waited: in all, during this instruction
        Duration waiting = Duration.ZERO;
        Duration waited = Duration.ZERO;
        while (left.isPositive()) {
            if (charging[rover] && levels[rover] >= RECHARGED * capacity) {
                charging[rover] = false;
            } else if (!charging[rover] && levels[rover] <= RESERVE * capacity) {
                charging[rover] = true;
            }
            Duration step = charging[rover] || STEP.compareTo(left) < 0 ? STEP : left;
            double used = charging[rover] ? PowerSystem.IDLE_WATTS : PowerSystem.ACTIVE_WATTS;
            double done = busy.isZero() ? 0 : 1 - left.toMillis() / (double) busy.toMillis();
            double produced = system.source().watts(sun, along(from, to, done), time);
            double hours = step.toMillis() / 3_600_000.0;
            levels[rover] = Math.clamp(levels[rover] + (produced - used) * hours, 0, capacity);
            if (charging[rover]) {
                waiting = waiting.plus(step);
                waited = waited.plus(step);
                if (waiting.compareTo(year) > 0) {
                    return Timing.stoppedAfter(time.plus(step).minus(start), waited);
                }
            } else {
                left = left.minus(step);
                waiting = Duration.ZERO;
            }
            time = time.plus(step);
        }
        return Timing.done(busy, waited);
    }

    // Where a rover is when it has done this part (from 0 to 1) of the way between two states.
    private Coordinates along(Rover from, Rover to, double done) {
        Coordinates start = planet.centerOf(from.position());
        Coordinates end = planet.centerOf(to.position());
        boolean acrossAPole = !from.position().equals(to.position()) && from.direction() != to.direction();
        if (acrossAPole) {
            return done < 0.5 ? start : end;
        }
        double eastward = ((end.longitude() - start.longitude()) % 360 + 540) % 360 - 180;
        return new Coordinates(start.latitude() + done * (end.latitude() - start.latitude()),
                start.longitude() + done * eastward);
    }
}
