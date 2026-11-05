package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The archive against a real PostgreSQL, in Docker. Disabled without Docker.
@Testcontainers(disabledWithoutDocker = true)
class JdbcArchiveIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @BeforeEach
    void emptyTheDatabase() throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
        }
        new JdbcArchive(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).close();
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private long count(String table) throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }

    @Test
    void messagesReadTwiceAreArchivedOnce() throws Exception {
        RoverTelemetry telemetry = new RoverTelemetry("m1", "Spirit", 0, 60, "Sol 0 00:01", 1, 2, 3, 4, "N", 90, false, 10);
        RoverEvent event = new RoverEvent("m1", "Spirit", 0, 0, "Sol 0 00:00", RoverEvent.Type.LANDED, "here");
        try (Archive archive = new JdbcArchive(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            for (int read = 0; read < 2; read++) {
                archive.telemetry(telemetry);
                archive.event(event);
                archive.status(new MissionStatus("m1", MissionStatus.Type.STARTED, "f", "Mars", 1, Instant.EPOCH));
                archive.commit();
            }
        }
        assertEquals(1, count("missions"));
        assertEquals(1, count("telemetry"));
        assertEquals(1, count("events"));
    }
}
