package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardTest {

    private final Dashboard dashboard = new Dashboard();

    private RoverTelemetry at(String mission, String rover, long seconds, double battery, boolean night) {
        return new RoverTelemetry(mission, rover, 0, seconds, "Sol " + seconds / 88_775 + " 10:00", -4.5, 137.4, 0, 0,
                "S", battery, night, 0, 0.5, 1);
    }

    @Test
    void showsTheLastStateOfEachRover() {
        dashboard.telemetry(at("m1", "Curiosity", 100, 50, false));
        dashboard.telemetry(at("m1", "Curiosity", 200, 80, true));
        List<String> screen = dashboard.render();
        assertEquals(2, screen.size());
        assertTrue(screen.get(0).contains("mission m1"), screen.get(0));
        assertTrue(screen.get(1).startsWith("Curiosity"), screen.get(1));
        assertTrue(screen.get(1).contains("########..  80 %  night  dust 0.5  active"), screen.get(1));
    }

    @Test
    void aRoverWithNoNewsForASolIsSilent() {
        dashboard.telemetry(at("m1", "Spirit", 0, 50, false));
        dashboard.telemetry(at("m1", "Opportunity", 200_000, 50, false));
        assertTrue(dashboard.render().get(2).endsWith("silent"), dashboard.render().get(2));
    }

    @Test
    void eventsGiveTheStatusOfARover() {
        dashboard.telemetry(at("m1", "Spirit", 0, 0, true));
        dashboard.event(new RoverEvent("m1", "Spirit", 1, 0, "Sol 0 00:00", RoverEvent.Type.STOPPED, "out of energy"));
        dashboard.event(new RoverEvent("m1", "Twin", 0, 0, "Sol 0 00:00", RoverEvent.Type.LOST, "landed on another rover"));
        List<String> screen = dashboard.render();
        assertTrue(screen.get(1).endsWith("stopped"), screen.get(1));
        assertTrue(screen.get(2).startsWith("Twin") && screen.get(2).endsWith("lost"), screen.get(2));
    }

    @Test
    void onceTheMissionHasEndedEveryRoverIsDone() {
        dashboard.telemetry(at("m1", "Spirit", 0, 50, false));
        dashboard.telemetry(at("m1", "Opportunity", 200_000, 50, false));
        dashboard.status(new MissionStatus("m1", MissionStatus.Type.ENDED, "f", "Mars", 88_775, 1, java.time.Instant.EPOCH));
        assertTrue(dashboard.render().get(2).endsWith("done"), dashboard.render().get(2));
    }

    @Test
    void aNewMissionReplacesTheOldOne() {
        dashboard.telemetry(at("m1", "Spirit", 0, 50, false));
        dashboard.telemetry(at("m2", "Curiosity", 0, 50, false));
        List<String> screen = dashboard.render();
        assertEquals(2, screen.size());
        assertTrue(screen.get(1).startsWith("Curiosity"));
    }
}
