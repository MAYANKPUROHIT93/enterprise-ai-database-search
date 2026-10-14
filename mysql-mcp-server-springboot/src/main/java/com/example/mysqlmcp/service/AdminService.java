package com.example.mysqlmcp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final DatabaseService databaseService;

    public AdminService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Map<String, Object> getServerStatus() throws SQLException {
        Map<String, Object> status = new LinkedHashMap<>();
        try (Connection conn = databaseService.getConnection(null);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SHOW GLOBAL STATUS WHERE Variable_name IN (
                     'Uptime', 'Threads_connected', 'Threads_running', 'Questions',
                     'Slow_queries', 'Innodb_buffer_pool_reads', 'Innodb_buffer_pool_read_requests'
                 )
             """)) {
            while (rs.next()) {
                status.put(rs.getString("Variable_name"), rs.getString("Value"));
            }
        }
        return status;
    }

    public Map<String, Object> getDatabaseStats(String database) throws SQLException {
        String filter = (database != null && !database.isBlank()) ? "WHERE table_schema = ?" : "WHERE table_schema NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')";
        String sql = """
            SELECT table_schema as database_name,
                   COUNT(table_name) as table_count,
                   ROUND(SUM(data_length) / 1024 / 1024, 2) as data_size_mb,
                   ROUND(SUM(index_length) / 1024 / 1024, 2) as index_size_mb,
                   ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as total_size_mb
            FROM information_schema.tables
            """ + filter + """
            GROUP BY table_schema
            ORDER BY total_size_mb DESC
        """;

        List<Map<String, Object>> stats = new ArrayList<>();
        try (Connection conn = databaseService.getConnection(null);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (database != null && !database.isBlank()) {
                ps.setString(1, database);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("database_name", rs.getString("database_name"));
                    row.put("table_count", rs.getInt("table_count"));
                    row.put("data_size_mb", rs.getDouble("data_size_mb"));
                    row.put("index_size_mb", rs.getDouble("index_size_mb"));
                    row.put("total_size_mb", rs.getDouble("total_size_mb"));
                    stats.add(row);
                }
            }
        }
        return Map.of("stats", stats);
    }

    public Map<String, Object> getProcessList() throws SQLException {
        List<Map<String, Object>> processes = new ArrayList<>();
        try (Connection conn = databaseService.getConnection(null);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW FULL PROCESSLIST")) {
            while (rs.next()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", rs.getLong("Id"));
                p.put("user", rs.getString("User"));
                p.put("host", rs.getString("Host"));
                p.put("db", rs.getString("db"));
                p.put("command", rs.getString("Command"));
                p.put("time", rs.getLong("Time"));
                p.put("state", rs.getString("State"));
                p.put("info", rs.getString("Info"));
                processes.add(p);
            }
        }
        return Map.of("processCount", processes.size(), "processes", processes);
    }

    public Map<String, Object> killQuery(long processId, Boolean killConnection) throws SQLException {
        if (databaseService.getConfig().isReadOnly()) {
            throw new SecurityException("Database is in READ_ONLY mode. Process termination is forbidden.");
        }

        String command = (killConnection != null && killConnection) ? "KILL CONNECTION " + processId : "KILL QUERY " + processId;
        try (Connection conn = databaseService.getConnection(null);
             Statement stmt = conn.createStatement()) {
            stmt.execute(command);
            return Map.of(
                    "success", true,
                    "processId", processId,
                    "action", (killConnection != null && killConnection) ? "KILL CONNECTION" : "KILL QUERY",
                    "message", "Successfully executed: " + command
            );
        }
    }

    public Map<String, Object> openInWorkbench(String sql, String filename) {
        String safeName = (filename != null && !filename.isBlank()) ? filename : "query_" + System.currentTimeMillis() + ".sql";
        File tempFile = new File(System.getProperty("java.io.tmpdir"), safeName);
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(sql);
        } catch (IOException e) {
            return Map.of("success", false, "error", "Failed to write SQL script: " + e.getMessage());
        }

        // Try launching MySQL Workbench
        boolean launched = false;
        try {
            new ProcessBuilder("mysqlworkbench", tempFile.getAbsolutePath()).start();
            launched = true;
        } catch (Exception ignored) {}

        return Map.of(
                "success", true,
                "filePath", tempFile.getAbsolutePath(),
                "workbenchLaunched", launched,
                "message", launched ? "Opened query in MySQL Workbench" : "Saved SQL file at: " + tempFile.getAbsolutePath()
        );
    }
}
