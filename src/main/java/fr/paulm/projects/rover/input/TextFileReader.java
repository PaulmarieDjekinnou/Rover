package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Reads a simulation from a file in text format: a mission to a planet when the file starts with a planet line
// (MissionParser), rovers on a plateau otherwise (TextFormatParser).
public final class TextFileReader implements SimulationReader {
    private final Path path;
    private final TextFormatParser parser = new TextFormatParser();
    private final MissionParser missionParser = new MissionParser(this::planet);

    public TextFileReader(Path path) {
        this.path = path;
    }

    @Override
    public Scenario read() {
        String content;
        try {
            content = Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (MissionParser.accepts(content)) {
            return new Scenario.OnPlanet(missionParser.parse(content));
        }
        return new Scenario.OnPlateau(parser.parse(content));
    }

    // The planet named in a mission file: "mars", or "map <file>" for a map drawn in text, next to the mission file.
    private Planet planet(String name) {
        if (name.equals("mars")) {
            return new MolaFileReader().mars();
        }
        if (name.startsWith("map ")) {
            Path map = path.resolveSibling(name.substring("map ".length()).trim());
            try {
                return new TextMapParser().parse(map.getFileName().toString(), Files.readString(map));
            } catch (IOException e) {
                throw new InvalidInputException("Cannot read the map file \"" + map + "\"");
            } catch (InvalidInputException e) {
                throw new InvalidInputException("In the map file \"" + map + "\": " + e.getMessage());
            }
        }
        throw new InvalidInputException("Unknown planet: \"" + name + "\". Expected: mars, or map followed by a file name");
    }
}
