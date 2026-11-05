package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.simulation.ClockSimulation;
import fr.paulm.projects.rover.domain.simulation.MissionResult;
import fr.paulm.projects.rover.input.Scenario;
import fr.paulm.projects.rover.input.TextFileReader;
import fr.paulm.projects.rover.mars.mission.Batteries;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.PlanetMission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

// The sample mission against real missions. Real rovers average some 9 m per sol over their missions: Curiosity
// 18.93 km in 2039 sols (JPL traverse map, May 2018), Opportunity 45.16 km in 5111 sols (June 2018). They drive no
// more than a few hours a day. The bounds allow a factor of about 3 either way: they catch a simulation that is ten
// times too fast or too slow, not a few percent.
class RealMissionsTest {

    @Test
    void roversDriveSomeMetersPerSolOnAverage() throws Exception {
        PlanetMission mission = ((Scenario.OnPlanet) new TextFileReader(TestResources.path("simulations/valid/mars.txt"))
                .read()).mission();
        Deployment deployment = mission.land();
        List<MissionResult> results = new ClockSimulation(new Batteries(deployment, mission.operations()))
                .run(deployment.input());
        double sol = deployment.planet().dayLength().toMillis();
        for (MissionResult result : results) {
            double sols = result.totalTime().toMillis() / sol;
            double metersPerSol = result.distanceMeters() / sols;
            assertTrue(metersPerSol >= 3 && metersPerSol <= 30, metersPerSol + " m per sol");
            // Driving a few hours a sol at most: the drive window is 5 hours
            assertTrue(result.drivingTime().toMillis() / sol <= sols * 5 / 24.0 + 1, "too many hours of driving");
        }
    }
}
