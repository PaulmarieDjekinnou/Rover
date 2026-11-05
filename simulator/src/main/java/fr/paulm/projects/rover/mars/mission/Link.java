package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.EarthMars;
import fr.paulm.projects.rover.mars.planet.Sun;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

// The link between the rovers and Earth. The date at the start of the mission (UTC) places the two planets, which
// gives the light delay; the relay orbiters pass over each rover at local times of its site, each pass carrying
// at most a number of messages; until a pass, a rover keeps its messages in a memory of limited size.
// By default, two orbiters pass twice a sol, early in the morning and late in the afternoon, as Mars Odyssey and
// Mars Reconnaissance Orbiter do.
public record Link(Instant start, List<Double> passHours, int messagesPerPass, int memoryMessages) {

    public static final List<Double> DEFAULT_PASSES = List.of(4.0, 5.5, 16.0, 17.5);
    public static final int DEFAULT_MESSAGES_PER_PASS = 500;
    public static final int DEFAULT_MEMORY_MESSAGES = 5000;
    // Radio contact is lost when Mars is this close to the Sun in the sky of Earth: about two weeks every 26 months
    public static final double CONJUNCTION_DEGREES = 2;

    public Link {
        passHours = List.copyOf(passHours);
        if (passHours.isEmpty() || passHours.stream().anyMatch(hours -> !(hours >= 0 && hours < 24))
                || messagesPerPass < 1 || memoryMessages < 1) {
            throw new InvalidInputException("Invalid link: passes at " + passHours + " hours, " + messagesPerPass
                    + " message(s) per pass, a memory of " + memoryMessages + " message(s). Expected: at least one pass"
                    + " within the day, and at least one message per pass and in memory");
        }
    }

    // The default link from that date.
    public Link(Instant start) {
        this(start, DEFAULT_PASSES, DEFAULT_MESSAGES_PER_PASS, DEFAULT_MEMORY_MESSAGES);
    }

    // The first pass over the site at that time of the mission or after.
    public Duration nextPass(Sun sun, Coordinates site, Duration after, Duration dayLength) {
        double now = sun.localTime(site, after);
        double hoursLater = passHours.stream().mapToDouble(hours -> ((hours - now) % 24 + 24) % 24).min().orElseThrow();
        return after.plusMillis(Math.round(hoursLater / 24 * dayLength.toMillis()));
    }

    // When a message sent from Mars at that time of the mission reaches Earth; none during a solar conjunction.
    public Optional<Duration> arrival(Duration sent) {
        EarthMars planets = new EarthMars(start.plus(sent));
        return planets.sunAngleDegrees() < CONJUNCTION_DEGREES
                ? Optional.empty() : Optional.of(sent.plus(planets.lightDelay()));
    }
}
