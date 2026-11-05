package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverPlanned;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

// The archive in PostgreSQL, with plain JDBC, in one transaction per poll. The schema is created and upgraded by
// Flyway when the archive opens (db/migration: V1 the tables, V2 the numbers of the events, V3 the plans of the rovers,
// the dust and the length of the sol, V4 when each message reached Earth).
// Telemetry and events read twice are written once, thanks to their primary keys (mission, rover, sequence).
// A mission is created by the first message that mentions it: the start of a mission and its telemetry come
// through different topics, so nothing says which one arrives first. The start then fills the mission in.
public final class JdbcArchive implements Archive {
    private final Connection connection;

    public JdbcArchive(String url, String user, String password) {
        Flyway.configure().dataSource(url, user, password).load().migrate();
        try {
            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect to the database " + url + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void status(MissionStatus message) {
        ensureMission(message.missionId());
        String column = message.type() == MissionStatus.Type.STARTED ? "started_at" : "ended_at";
        update("UPDATE missions SET file = ?, planet = ?, sol_seconds = ?, seed = ?, " + column + " = ? WHERE mission_id = ?",
                message.file(), message.planet(), message.solSeconds(), message.seed(), Timestamp.from(message.wallClock()),
                message.missionId());
    }

    @Override
    public void plan(RoverPlanned m) {
        ensureMission(m.missionId());
        update("""
                INSERT INTO rovers (mission_id, rover, site_latitude, site_longitude, power, battery_watt_hours,
                                    landing_seconds, waypoints)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING""",
                m.missionId(), m.rover(), m.siteLatitude(), m.siteLongitude(), m.power(), m.batteryWattHours(),
                m.landingSeconds(), m.waypoints());
    }

    @Override
    public void telemetry(RoverTelemetry m) {
        ensureMission(m.missionId());
        update("""
                INSERT INTO telemetry (mission_id, rover, sequence, mission_seconds, mission_clock, latitude, longitude,
                                       cell_x, cell_y, direction, battery_percent, night, distance_meters,
                                       dust_opacity, panel_cleanliness, received_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING""",
                m.missionId(), m.rover(), m.sequence(), m.missionSeconds(), m.missionClock(), m.latitude(), m.longitude(),
                m.cellX(), m.cellY(), m.direction(), m.batteryPercent(), m.night(), m.distanceMeters(), m.dustOpacity(),
                m.panelCleanliness(), m.receivedSeconds());
    }

    @Override
    public void event(RoverEvent m) {
        ensureMission(m.missionId());
        update("""
                INSERT INTO events (mission_id, rover, sequence, mission_seconds, mission_clock, type, detail,
                                    received_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING""",
                m.missionId(), m.rover(), m.sequence(), m.missionSeconds(), m.missionClock(), m.type().name(), m.detail(),
                m.receivedSeconds());
    }

    @Override
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot commit to the database: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot close the database connection: " + e.getMessage(), e);
        }
    }

    private void ensureMission(String missionId) {
        update("INSERT INTO missions (mission_id) VALUES (?) ON CONFLICT DO NOTHING", missionId);
    }

    private void update(String sql, Object... values) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot write to the database: " + e.getMessage(), e);
        }
    }
}
