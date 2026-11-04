package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestResources;
import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.mars.mission.MarsMission;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextFileReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsAndParsesTheFile() throws Exception {
        Scenario scenario = new TextFileReader(TestResources.path("simulations/valid/sample.txt")).read();
        SimulationInput input = assertInstanceOf(Scenario.OnPlateau.class, scenario).input();
        assertEquals(2, input.missions().size());
        assertEquals(new Rover(new Position(1, 2), Direction.N), input.missions().get(0).rover());
        assertEquals(new Rover(new Position(3, 3), Direction.E), input.missions().get(1).rover());
    }

    @Test
    void missingFileIsReportedAsUnreadable() {
        Path missing = tempDir.resolve("absent.txt");
        assertThrows(UncheckedIOException.class, () -> new TextFileReader(missing).read());
    }

    @Test
    void invalidContentErrorIsPropagated() throws Exception {
        // Format messages are tested in TextFormatParserTest: this only checks the reader lets the error through.
        TextFileReader reader = new TextFileReader(TestResources.path("simulations/invalid/unknown-instruction.txt"));
        assertEquals("Line 5: Unknown instruction: \"X\". Expected: L, R, M or B",
                assertThrows(InvalidInputException.class, reader::read).getMessage());
    }

    @Test
    void aFileStartingWithAPlanetIsAMission() throws Exception {
        Scenario scenario = new TextFileReader(TestResources.path("simulations/valid/mini-planet.txt")).read();
        MarsMission mission = assertInstanceOf(Scenario.OnMars.class, scenario).mission();
        // The map is read from the file named in the mission, next to it
        assertEquals("mini-planet.map.txt", mission.planet().name());
        assertEquals(List.of("Polar", "Climber", "Twin"), mission.rovers().stream().map(RoverPlan::name).toList());
    }

    @Test
    void aMissingMapIsReportedOnThePlanetLine() throws Exception {
        Path mission = tempDir.resolve("mission.txt");
        Files.writeString(mission, "planet map absent.txt\n");
        String message = assertThrows(InvalidInputException.class, () -> new TextFileReader(mission).read()).getMessage();
        assertEquals("Line 1: Cannot read the map file \"" + tempDir.resolve("absent.txt") + "\"", message);
    }
}
