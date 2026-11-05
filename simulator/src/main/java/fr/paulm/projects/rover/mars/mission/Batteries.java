package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.Pace;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;
import fr.paulm.projects.rover.mars.rover.PowerSource;
import fr.paulm.projects.rover.mars.rover.PowerSystem;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

// The batteries of the rovers that landed, followed through the mission: the pace of the rovers on the clock.
// Each battery starts full. It is charged by the power source of its rover, and drained by the rover: more when it
// drives or turns (PowerSystem.ACTIVE_WATTS) than when it waits (PowerSystem.IDLE_WATTS).
// A rover works while its battery holds more than a reserve (a fifth of its capacity). Below it, the rover waits
// and recharges until its battery is four fifths full, then goes on: a solar rover drives by day, and spends its
// nights waiting. A rover whose battery does not reach that level in a whole year of waiting is stopped for good,
// as Spirit was in 2010.
// Solar panels gather dust: they lose 0.15 % of their power a sol, three times faster in a dust storm, until the wind
// cleans them (about once in 60 sols, drawn from the seed, giving back 30 % of their power), as the "cleaning
// events" that kept Spirit and Opportunity going. These rates are orders of magnitude, not a fit to the missions.
// The operations add their rules (see Operations): a rover drives only in its drive window, at its local time,
// and at most its distance per sol; otherwise it waits, using its idle power, until the next window or sol.
// The power is computed step by step (10 minutes), where the rover is: between the centers of the cell it leaves
// and of the cell it goes to, as far along as it has driven (across a pole, on one side for the first half of the
// way, on the other side for the second half).
public final class Batteries implements Pace {

    private static final Duration STEP = Duration.ofMinutes(10);
    private static final double RESERVE = 0.2;
    private static final double RECHARGED = 0.8;
    private static final double DUSTING_PER_SOL = 0.0015;
    private static final double STORM_OPACITY = 2;
    private static final double CLEANINGS_PER_SOL = 1 / 60.0;
    private static final double CLEANING = 0.3;

    private final Planet planet;
    private final Sun sun;
    private final List<Operations> operations;
    private final List<PowerSystem> systems;
    private final List<Duration> starts;
    private final double[] levels;
    private final boolean[] charging;
    // The local sol of each rover (counted from the start of the mission), and how far it has driven in it
    private final long[] sols;
    private final double[] drivenThisSol;
    // How clean the solar panels of each rover are, from 0 to 1, and the draws of the wind that cleans them
    private final double[] cleanliness;
    private final List<SplittableRandom> winds = new ArrayList<>();
    // The level of each battery as it went, at each step: the history needs it at any time
    private final List<TreeMap<Duration, Double>> histories = new ArrayList<>();

    // The batteries alone, with no limit from the operations.
    public Batteries(Deployment deployment) {
        this(deployment, Operations.UNLIMITED);
    }

    // With the operations, and no seed for the wind.
    public Batteries(Deployment deployment, Operations operations) {
        this(deployment, operations, 0);
    }

    public Batteries(Deployment deployment, Operations operations, long seed) {
        this.planet = deployment.planet();
        this.sun = deployment.sun();
        // Each rover is operated as its plan says, or else as the mission says
        this.operations = deployment.landed().stream()
                .map(touchdown -> touchdown.plan().operations().orElse(operations)).toList();
        this.systems = deployment.landed().stream().map(touchdown -> touchdown.plan().power()).toList();
        this.starts = deployment.landed().stream().map(Touchdown::time).toList();
        this.levels = systems.stream().mapToDouble(PowerSystem::batteryWattHours).toArray();
        this.charging = new boolean[systems.size()];
        this.sols = new long[systems.size()];
        this.drivenThisSol = new double[systems.size()];
        this.cleanliness = new double[systems.size()];
        Arrays.fill(cleanliness, 1);
        for (int i = 0; i < systems.size(); i++) {
            winds.add(new SplittableRandom(seed * 31 + i));
            TreeMap<Duration, Double> history = new TreeMap<>();
            history.put(starts.get(i), levels[i]);
            histories.add(history);
        }
    }

    // A rover starts when it lands.
    @Override
    public Duration start(int rover) {
        return starts.get(rover);
    }

    // How clean the solar panels of a rover are after its last instruction, from 0 to 1 (1 without panels).
    public double panelCleanliness(int rover) {
        return cleanliness[rover];
    }

    // How full the battery of a rover is after its last instruction, from 0 to 1.
    public double charge(int rover) {
        return levels[rover] / systems.get(rover).batteryWattHours();
    }

    // How full the battery of a rover was at a time of the clock, from 0 to 1 (full before it landed).
    // Known up to the end of the last instruction the clock asked about.
    public double charge(int rover, Duration time) {
        Map.Entry<Duration, Double> level = histories.get(rover).floorEntry(time);
        return level == null ? 1 : level.getValue() / systems.get(rover).batteryWattHours();
    }

    @Override
    public Timing timing(int rover, Rover from, Rover to, Duration start, Duration busy) {
        PowerSystem system = systems.get(rover);
        double capacity = system.batteryWattHours();
        Duration year = Duration.ofMillis(Math.round(planet.orbit().yearInSols() * planet.dayLength().toMillis()));
        Coordinates place = planet.centerOf(from.position());
        double distance = from.position().equals(to.position()) ? 0 : planet.distance(from.position(), to.position());
        double metersPerMilli = busy.isZero() ? 0 : distance / busy.toMillis();
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
            long sol = localSol(place, time);
            if (sol != sols[rover]) {
                sols[rover] = sol;
                drivenThisSol[rover] = 0;
            }
            Operations rules = operations.get(rover);
            boolean allowed = rules.inWindow(sun.localTime(place, time)) && drivenThisSol[rover] < rules.maxMetersPerSol();
            boolean working = !charging[rover] && allowed;
            Duration step = !working || STEP.compareTo(left) < 0 ? STEP : left;
            if (working && metersPerMilli > 0) {
                // The last step of the sol stops at the distance of the sol
                long untilTheLimit = (long) Math.ceil((operations.get(rover).maxMetersPerSol() - drivenThisSol[rover])
                        / metersPerMilli);
                if (untilTheLimit < step.toMillis()) {
                    step = Duration.ofMillis(Math.max(1, untilTheLimit));
                }
            }
            double used = working ? PowerSystem.ACTIVE_WATTS : PowerSystem.IDLE_WATTS;
            double done = busy.isZero() ? 0 : 1 - left.toMillis() / (double) busy.toMillis();
            double produced = system.source().watts(sun, along(from, to, done), time);
            if (system.source() instanceof PowerSource.SolarPanels) {
                produced *= cleanliness[rover];
                gatherDust(rover, step, time);
            }
            double hours = step.toMillis() / 3_600_000.0;
            levels[rover] = Math.clamp(levels[rover] + (produced - used) * hours, 0, capacity);
            histories.get(rover).put(time.plus(step), levels[rover]);
            if (!working) {
                waiting = waiting.plus(step);
                waited = waited.plus(step);
                if (waiting.compareTo(year) > 0) {
                    return Timing.stoppedAfter(time.plus(step).minus(start), waited);
                }
            } else {
                left = left.minus(step);
                drivenThisSol[rover] += metersPerMilli * step.toMillis();
                waiting = Duration.ZERO;
            }
            time = time.plus(step);
        }
        return Timing.done(busy, waited);
    }

    // The dust settles on the panels during a step, faster in a storm, and the wind may clean them.
    private void gatherDust(int rover, Duration step, Duration time) {
        double sols = step.toMillis() / (double) planet.dayLength().toMillis();
        double rate = DUSTING_PER_SOL * (sun.opticalDepth(time) > STORM_OPACITY ? 3 : 1);
        cleanliness[rover] = Math.max(0, cleanliness[rover] - rate * sols);
        if (winds.get(rover).nextDouble() < CLEANINGS_PER_SOL * sols) {
            cleanliness[rover] = Math.min(1, cleanliness[rover] + CLEANING);
        }
    }

    // The local sol at a place: it changes at local midnight.
    private long localSol(Coordinates place, Duration time) {
        double hoursAtStart = sun.localTime(place, Duration.ZERO);
        double hours = time.toMillis() * 24.0 / planet.dayLength().toMillis();
        return (long) Math.floor((hoursAtStart + hours) / 24);
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
