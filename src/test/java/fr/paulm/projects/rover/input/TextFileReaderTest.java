package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.TestResources;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.SimulationInput;
import fr.paulm.projects.rover.domain.enums.Direction;
import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    // Complete files from simulations/invalid: the format error is propagated with its message.

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "missing-instructions              | Each rover needs a position line and an instruction line",
            "missing-instructions-second-rover | Each rover needs a position line and an instruction line",
            "outside-plateau                   | Rover starts outside the plateau: \"6 2\"",
            "unknown-instruction               | Unknown instruction: \"X\". Expected: L, R or M",
    })
    void invalidFile(String name, String message) throws Exception {
        TextFileReader reader = new TextFileReader(TestResources.path("simulations/invalid/" + name + ".txt"));
        assertEquals(message, assertThrows(InvalidInputException.class, reader::read).getMessage());
    }
}
