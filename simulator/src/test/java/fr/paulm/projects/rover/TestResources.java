package fr.paulm.projects.rover;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class TestResources {

    private TestResources() {
    }

    // Path of a file in src/test/resources, resolved from the classpath.
    public static Path path(String name) throws URISyntaxException {
        URL url = TestResources.class.getResource("/" + name);
        assertNotNull(url, "Test file not found: " + name);
        return Path.of(url.toURI());
    }

    public static List<String> read(String name) throws IOException, URISyntaxException {
        return Files.readAllLines(path(name));
    }
}
