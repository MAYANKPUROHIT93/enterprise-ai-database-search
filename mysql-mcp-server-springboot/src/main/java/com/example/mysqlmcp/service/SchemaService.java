package com.example.mysqlmcp.service;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class SchemaService {

    private final DatabaseService databaseService;

    public SchemaService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Map<String, Object> listDatabases() throws SQLException {
        String sql = """
            SELECT SCHEMA_NAME as database_name, DEFAULT_CHARACTER_SET_NAME as charset, DEFAULT_COLLATION_NAME as collation
            FROM information_schema.SCHEMATA
            ORDER BY SCHEMA_NAME
        """;
        List<Map<String, Object>> databases = new ArrayList<>();
        try (Connection conn = databaseService.getConnection(null);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> db = new LinkedHashMap<>();
                db.put("database_name", rs.getString("database_name"));
                db.put("charset", rs.getString("charset"));
                db.put("collation", rs.getString("collation"));
                databases.add(db);
            }
        }
        return Map.of("databases", databases);
    }

    public Map<String, Object> listTables(String database) throws SQLException {
        String targetDb = (database != null && !database.isBlank()) ? database : databaseService.getConfig().getDatabase();
        if (targetDb == null || targetDb.isBlank()) {
            throw new IllegalArgumentException("No database specified and no default database configured.");
        }

        String sql = """
            SELECT TABLE_NAME as table_name, TABLE_TYPE as table_type, ENGINE as engine,
                   TABLE_ROWS as approx_rows, DATA_LENGTH as data_bytes, INDEX_LENGTH as index_bytes,
                   TABLE_COMMENT as comment
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = ?
            ORDER BY TABLE_NAME
        """;
        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection conn = databaseService.getConnection(null);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetDb);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("table_name", rs.getString("table_name"));
                    t.put("table_type", rs.getString("table_type"));
                    t.put("engine", rs.getString("engine"));
                    t.put("approx_rows", rs.getLong("approx_rows"));
                    t.put("data_bytes", rs.getLong("data_bytes"));
                    t.put("index_bytes", rs.getLong("index_bytes"));
                    t.put("comment", rs.getString("comment"));
                    tables.add(t);
                }
            }
        }
        return Map.of("database", targetDb, "tables", tables);
    }

    public Map<String, Object> describeTable(String table, String database) throws SQLException {
        String targetDb = (database != null && !database.isBlank()) ? database : databaseService.getConfig().getDatabase();
        String sql = """
            SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY,
                   COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
        """;
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = databaseService.getConnection(null);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetDb);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("column_name", rs.getString("COLUMN_NAME"));
                    col.put("data_type", rs.getString("DATA_TYPE"));
                    col.put("column_type", rs.getString("COLUMN_TYPE"));
                    col.put("nullable", "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                    col.put("key", rs.getString("COLUMN_KEY"));
                    col.put("default_value", rs.getString("COLUMN_DEFAULT"));
                    col.put("extra", rs.getString("EXTRA"));
                    col.put("comment", rs.getString("COLUMN_COMMENT"));
                    columns.add(col);
                }
            }
        }
        return Map.of("database", targetDb, "table", table, "columns", columns);
    }

    public Map<String, Object> showCreateTable(String table, String database) throws SQLException {
        String targetDb = (database != null && !database.isBlank()) ? database : databaseService.getConfig().getDatabase();
        String safeIdentifier = (targetDb != null && !targetDb.isBlank())
                ? "`" + targetDb.replace("`", "``") + "`.`" + table.replace("`", "``") + "`"
                : "`" + table.replace("`", "``") + "`";

        String ddl = "";
        try (Connection conn = databaseService.getConnection(targetDb);
             PreparedStatement ps = conn.prepareStatement("SHOW CREATE TABLE " + safeIdentifier);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                ddl = rs.getString(2);
            }
        } catch (SQLException e) {
            // Fallback for views
            try (Connection conn = databaseService.getConnection(targetDb);
                 PreparedStatement ps = conn.prepareStatement("SHOW CREATE VIEW " + safeIdentifier);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ddl = rs.getString(2);
                } else {
                    throw e;
                }
            }
        }
        return Map.of("database", targetDb, "table", table, "ddl", ddl);
    }

    public Map<String, Object> listIndexes(String table, String database) throws SQLException {
        String targetDb = (database != null && !database.isBlank()) ? database : databaseService.getConfig().getDatabase();
        String sql = """
            SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, NON_UNIQUE, INDEX_TYPE, CARDINALITY, NULLABLE
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            ORDER BY INDEX_NAME, SEQ_IN_INDEX
        """;
        List<Map<String, Object>> indexes = new ArrayList<>();
        try (Connection conn = databaseService.getConnection(null);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetDb);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> idx = new LinkedHashMap<>();
                    idx.put("index_name", rs.getString("INDEX_NAME"));
                    idx.put("column_name", rs.getString("COLUMN_NAME"));
                    idx.put("seq_in_index", rs.getInt("SEQ_IN_INDEX"));
                    idx.put("unique", rs.getInt("NON_UNIQUE") == 0);
                    idx.put("index_type", rs.getString("INDEX_TYPE"));
                    idx.put("cardinality", rs.getLong("CARDINALITY"));
                    indexes.add(idx);
                }
            }
        }
        return Map.of("database", targetDb, "table", table, "indexes", indexes);
    }

    public Map<String, Object> listForeignKeys(String table, String database) throws SQLException {
        String targetDb = (database != null && !database.isBlank()) ? database : databaseService.getConfig().getDatabase();
        String sql = """
            SELECT k.CONSTRAINT_NAME, k.COLUMN_NAME, k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME,
                   r.UPDATE_RULE, r.DELETE_RULE
            FROM information_schema.KEY_COLUMN_USAGE k
            JOIN information_schema.REFERENTIAL_CONSTRAINTS r
              ON k.CONSTRAINT_NAME = r.CONSTRAINT_NAME AND k.CONSTRAINT_SCHEMA = r.CONSTRAINT_SCHEMA
            WHERE k.TABLE_SCHEMA = ? AND k.TABLE_NAME = ? AND k.REFERENCED_TABLE_NAME IS NOT NULL
        """;
        List<Map<String, Object>> fks = new ArrayList<>();
        try (Connection conn = databaseService.getConnection(null);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetDb);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fk = new LinkedHashMap<>();
                    fk.put("constraint_name", rs.getString("CONSTRAINT_NAME"));
                    fk.put("column_name", rs.getString("COLUMN_NAME"));
                    fk.put("referenced_table", rs.getString("REFERENCED_TABLE_NAME"));
                    fk.put("referenced_column", rs.getString("REFERENCED_COLUMN_NAME"));
                    fk.put("update_rule", rs.getString("UPDATE_RULE"));
                    fk.put("delete_rule", rs.getString("DELETE_RULE"));
                    fks.add(fk);
                }
            }
        }
        return Map.of("database", targetDb, "table", table, "foreign_keys", fks);
    }
}
