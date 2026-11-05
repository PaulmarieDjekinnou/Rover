package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.input.MolaFileReader;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.rover.ReliefTerrain;
import fr.paulm.projects.rover.mars.rover.Mobility;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.domain.InvalidInputException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The default rules on the real map of Mars: the sites of real rovers are accepted, famous high places are not.
class RealLandingSitesTest {

    private static Landing landing;

    @BeforeAll
    static void readMars() {
        Planet mars = new MolaFileReader().mars();
        landing = new Landing(mars, new ReliefTerrain(mars, Mobility.DEFAULT), LandingRules.DEFAULT);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"Curiosity (Gale), -4.59, 137.44", "Perseverance (Jezero), 18.38, 77.58",
            "Opportunity (Meridiani), -1.95, 354.47", "Spirit (Gusev), -14.57, 175.47"})
    void realLandingSitesAreAccepted(String rover, double latitude, double longitude) {
        assertDoesNotThrow(() -> landing.check(new Coordinates(latitude, longitude)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"Olympus Mons, 18.65, 226.2", "Tharsis plateau, 0, 250", "North pole, 85, 0"})
    void highOrPolarPlacesAreRefused(String place, double latitude, double longitude) {
        assertThrows(InvalidInputException.class, () -> landing.check(new Coordinates(latitude, longitude)));
    }
}
