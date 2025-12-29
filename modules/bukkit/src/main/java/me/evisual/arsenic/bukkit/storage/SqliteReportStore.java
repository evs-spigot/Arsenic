package me.evisual.arsenic.bukkit.storage;

import me.evisual.arsenic.bukkit.ConfigService;
import me.evisual.arsenic.core.alert.Alert;
import me.evisual.arsenic.core.storage.AlertLogEntry;
import me.evisual.arsenic.core.storage.AlertLogSort;
import me.evisual.arsenic.core.storage.PlayerReport;
import me.evisual.arsenic.core.storage.ReportStore;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteReportStore implements ReportStore {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private Connection connection;

    public SqliteReportStore(JavaPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    @Override
    public void init() throws Exception {
        plugin.getDataFolder().mkdirs();
        String fileName = configService.getString("database.sqlite.file", "arsenic.db");
        File file = new File(plugin.getDataFolder(), fileName);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        String url = "jdbc:sqlite:" + file.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        createSchema();
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Override
    public synchronized void recordAlert(Alert alert) throws Exception {
        String insertSql = "INSERT OR IGNORE INTO player_reports "
                + "(uuid, name, total_alerts, last_alert_ts, last_check, last_detail, last_severity) "
                + "VALUES (?, ?, 0, 0, '', '', '')";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setString(1, alert.getPlayerId().toString());
            insert.setString(2, alert.getPlayerName());
            insert.executeUpdate();
        }

        String updateSql = "UPDATE player_reports SET name = ?, total_alerts = total_alerts + 1, "
                + "last_alert_ts = ?, last_check = ?, last_detail = ?, last_severity = ? WHERE uuid = ?";
        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
            update.setString(1, alert.getPlayerName());
            update.setLong(2, alert.getTimestampMillis());
            update.setString(3, alert.getCheckName());
            update.setString(4, alert.getDetail());
            update.setString(5, alert.getSeverity().name());
            update.setString(6, alert.getPlayerId().toString());
            update.executeUpdate();
        }

        String logSql = "INSERT INTO alert_logs "
                + "(uuid, name, check_name, violation_level, severity, detail, alert_ts) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement logStmt = connection.prepareStatement(logSql)) {
            logStmt.setString(1, alert.getPlayerId().toString());
            logStmt.setString(2, alert.getPlayerName());
            logStmt.setString(3, alert.getCheckName());
            logStmt.setInt(4, alert.getViolationLevel());
            logStmt.setString(5, alert.getSeverity().name());
            logStmt.setString(6, alert.getDetail());
            logStmt.setLong(7, alert.getTimestampMillis());
            logStmt.executeUpdate();
        }
    }

    @Override
    public synchronized Optional<PlayerReport> findReport(UUID playerId) throws Exception {
        String sql = "SELECT uuid, name, total_alerts, last_alert_ts, last_check, last_detail, last_severity "
                + "FROM player_reports WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PlayerReport(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getInt("total_alerts"),
                        rs.getLong("last_alert_ts"),
                        rs.getString("last_check"),
                        rs.getString("last_detail"),
                        rs.getString("last_severity")
                ));
            }
        }
    }

    @Override
    public synchronized List<AlertLogEntry> listAlerts(UUID playerId, AlertLogSort sort, int limit, int offset)
            throws Exception {
        String orderBy = "alert_ts DESC";
        if (sort == AlertLogSort.HIGHEST_VL) {
            orderBy = "violation_level DESC, alert_ts DESC";
        } else if (sort == AlertLogSort.CHECK_NAME) {
            orderBy = "check_name ASC, alert_ts DESC";
        }

        String sql = "SELECT uuid, name, check_name, violation_level, severity, detail, alert_ts "
                + "FROM alert_logs WHERE uuid = ? ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
        List<AlertLogEntry> entries = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new AlertLogEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getString("check_name"),
                            rs.getInt("violation_level"),
                            rs.getString("severity"),
                            rs.getString("detail"),
                            rs.getLong("alert_ts")
                    ));
                }
            }
        }
        return entries;
    }

    @Override
    public synchronized int countAlerts(UUID playerId) throws Exception {
        String sql = "SELECT COUNT(*) AS total FROM alert_logs WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    @Override
    public synchronized void clearAlerts(UUID playerId) throws Exception {
        String deleteLogs = "DELETE FROM alert_logs WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteLogs)) {
            stmt.setString(1, playerId.toString());
            stmt.executeUpdate();
        }

        String resetReport = "UPDATE player_reports SET total_alerts = 0, last_alert_ts = 0, "
                + "last_check = '', last_detail = '', last_severity = '' WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(resetReport)) {
            stmt.setString(1, playerId.toString());
            stmt.executeUpdate();
        }
    }

    @Override
    public synchronized void clearAll() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM alert_logs");
            stmt.executeUpdate("DELETE FROM player_reports");
        }
    }

    private void createSchema() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_reports ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "name TEXT,"
                    + "total_alerts INTEGER NOT NULL,"
                    + "last_alert_ts INTEGER NOT NULL,"
                    + "last_check TEXT,"
                    + "last_detail TEXT,"
                    + "last_severity TEXT"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS alert_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "uuid TEXT NOT NULL,"
                    + "name TEXT,"
                    + "check_name TEXT,"
                    + "violation_level INTEGER NOT NULL,"
                    + "severity TEXT,"
                    + "detail TEXT,"
                    + "alert_ts INTEGER NOT NULL"
                    + ")");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_alert_logs_uuid_ts "
                    + "ON alert_logs(uuid, alert_ts)");
        }
    }
}
