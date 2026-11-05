package fr.paulm.projects.rover.telemetry;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.Ellipse;
import fr.paulm.projects.rover.mars.mission.LandingRules;
import fr.paulm.projects.rover.mars.mission.LandingSite;
import fr.paulm.projects.rover.mars.mission.Link;
import fr.paulm.projects.rover.mars.mission.PlanetMission;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import fr.paulm.projects.rover.mars.planet.EarthMars;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import fr.paulm.projects.rover.mars.rover.Mobility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownlinkTest {

    private static final Planet PLANET = TestPlanets.planet(10_000, 1_000, "0000", "0000");
    // The landing is at noon, local time, at the site of the rover: the only pass of the sol is an hour later
    private static final Instant START = Instant.parse("2012-08-06T00:00:00Z");
    private static final Deployment DEPLOYMENT = new PlanetMission(PLANET, Mobility.DEFAULT, new LandingRules(500, 60), 0,
            List.of(new RoverPlan("Spirit", new LandingSite(PLANET.centerOf(new Position(1, 1)), Ellipse.NONE),
                    Direction.N, List.of()))).land();
    private static final long SOL = PLANET.dayLength().toSeconds();
    private static final long PASS = SOL / 24;

    private final InMemorySink earth = new InMemorySink();

    private Downlink downlink(Instant start, int perPass, int memory) {
        return new Downlink(earth, new Link(start, List.of(13.0), perPass, memory), DEPLOYMENT);
    }

    private static RoverTelemetry telemetry(long sequence, long seconds) {
        return new RoverTelemetry("m1", "Spirit", sequence, seconds, "Sol 0", 0, 0, 1, 1, "N", 90, false, 0, 0.5, 1);
    }

    private static RoverEvent event(long sequence, long seconds) {
        return new RoverEvent("m1", "Spirit", sequence, seconds, "Sol 0", RoverEvent.Type.BLOCKED_BY_TERRAIN, "blocked");
    }

    private static long lightDelay(Instant start, long seconds) {
        return new EarthMars(start.plusSeconds(seconds)).lightDelay().toSeconds();
    }

    @Test
    void aMessageWaitsForThePassOfAnOrbiterThenForTheLight() {
        Downlink downlink = downlink(START, 10, 100);
        downlink.telemetry(telemetry(0, 60));
        assertEquals(List.of(), earth.telemetry);
        // Two hours later, the pass is over and light has had the time to reach Earth: 13.8 minutes when Curiosity
        // landed
        downlink.telemetry(telemetry(1, 2 * PASS));
        assertEquals(1, earth.telemetry.size());
        RoverTelemetry received = earth.telemetry.getFirst();
        assertEquals(60, received.missionSeconds());
        assertEquals(PASS + lightDelay(START, PASS), received.receivedSeconds(), 2);
        assertEquals(13.8, (received.receivedSeconds() - PASS) / 60.0, 0.3);
    }

    @Test
    void aPassCarriesOnlySoManyMessages() {
        Downlink downlink = downlink(START, 2, 100);
        for (int i = 0; i < 5; i++) {
            downlink.telemetry(telemetry(i, 60 + i));
        }
        downlink.telemetry(telemetry(5, SOL + 2 * PASS));
        // Two at the pass of the first sol, two at the next one; the last ones wait
        assertEquals(List.of(0L, 1L, 2L, 3L), earth.telemetry.stream().map(RoverTelemetry::sequence).toList());
        assertTrue(earth.telemetry.get(2).receivedSeconds() > SOL);
    }

    @Test
    void aFullMemoryLosesItsOldestTelemetryFirst() {
        Downlink downlink = downlink(START, 10, 2);
        downlink.event(event(0, 10));
        downlink.telemetry(telemetry(0, 20));
        downlink.telemetry(telemetry(1, 30));
        downlink.close();
        assertEquals(1, downlink.lostMessages());
        assertEquals(List.of(1L), earth.telemetry.stream().map(RoverTelemetry::sequence).toList());
        assertEquals(1, earth.events.size());
    }

    @Test
    void duringASolarConjunctionNothingReachesEarth() {
        // Mars was behind the Sun on 18 November 2023
        Instant conjunction = Instant.parse("2023-11-16T00:00:00Z");
        Downlink downlink = downlink(conjunction, 10, 100);
        downlink.telemetry(telemetry(0, 60));
        downlink.telemetry(telemetry(1, 5 * SOL));
        assertEquals(List.of(), earth.telemetry);
        downlink.close();
        assertEquals(2, earth.telemetry.size());
        long received = earth.telemetry.getFirst().receivedSeconds();
        assertTrue(received > 5 * SOL, "received at " + received);
        assertTrue(new EarthMars(conjunction.plusSeconds(received)).sunAngleDegrees() >= Link.CONJUNCTION_DEGREES - 0.1);
    }

    @Test
    void whatIsStillOnItsWayArrivesBeforeTheEndOfTheMission() {
        Downlink downlink = downlink(START, 10, 100);
        MissionStatus started = new MissionStatus("m1", MissionStatus.Type.STARTED, "f", "Mars", SOL, 0, START);
        MissionStatus ended = new MissionStatus("m1", MissionStatus.Type.ENDED, "f", "Mars", SOL, 0, START);
        downlink.status(started);
        assertEquals(List.of(started), earth.statuses);
        downlink.telemetry(telemetry(0, 60));
        downlink.event(event(0, 70));
        downlink.status(ended);
        assertEquals(1, earth.telemetry.size());
        assertEquals(1, earth.events.size());
        assertEquals(List.of(started, ended), earth.statuses);
    }

    @Test
    void theEventsOfARoverLostAtLandingOnlyWaitForTheLight() {
        Downlink downlink = downlink(START, 10, 100);
        downlink.event(new RoverEvent("m1", "Twin", 0, 0, "Sol 0", RoverEvent.Type.LOST, "landed on another rover"));
        downlink.telemetry(telemetry(0, Duration.ofMinutes(30).toSeconds()));
        assertEquals(1, earth.events.size());
        assertEquals(lightDelay(START, 0), earth.events.getFirst().receivedSeconds(), 1);
    }
}
