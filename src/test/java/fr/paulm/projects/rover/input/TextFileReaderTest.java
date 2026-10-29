package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestResources;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextFileReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsAndParsesTheFile() throws Exception {
        SimulationInput input = new TextFileReader(TestResources.path("simulations/valid/sample.txt")).read();
        assertEquals(2, input.missions().size());
        assertEquals(new Rover(1, 2, Direction.N), input.missions().get(0).rover());
        assertEquals(new Rover(3, 3, Direction.E), input.missions().get(1).rover());
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
        assertEquals("Unknown instruction: \"X\". Expected: L, R or M",
                assertThrows(InvalidInputException.class, reader::read).getMessage());
    }
}
