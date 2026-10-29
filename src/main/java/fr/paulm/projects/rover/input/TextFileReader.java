package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.SimulationInput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Reads a simulation from a file in text format. Parsing is delegated to TextFormatParser.
public final class TextFileReader implements SimulationReader {
    private final Path path;
    private final TextFormatParser parser = new TextFormatParser();

    public TextFileReader(Path path) {
        this.path = path;
    }

    @Override
    public SimulationInput read() {
        try {
            return parser.parse(Files.readString(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
