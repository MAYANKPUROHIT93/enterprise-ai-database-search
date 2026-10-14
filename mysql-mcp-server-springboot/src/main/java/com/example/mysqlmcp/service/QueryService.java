package com.example.mysqlmcp.service;

import com.example.mysqlmcp.util.TableFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    private static final Pattern DISALLOWED_READONLY_PATTERN =
            Pattern.compile("^\\s*(insert|update|delete|drop|alter|truncate|create|rename|grant|revoke|replace|lock)\\b", Pattern.CASE_INSENSITIVE);

    private final DatabaseService databaseService;

    public QueryService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Map<String, Object> executeQuery(String sql, List<Object> params, String database, Integer maxRows, String format) throws SQLException {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be empty.");
        }
        String trimmed = sql.trim();

        if (databaseService.getConfig().isReadOnly() && DISALLOWED_READONLY_PATTERN.matcher(trimmed).find()) {
            throw new SecurityException("Database is in READ_ONLY mode. Write operations are forbidden.");
        }

        int limit = (maxRows != null && maxRows > 0) ? maxRows : databaseService.getConfig().getMaxRows();
        long start = System.currentTimeMillis();

        try (Connection conn = databaseService.getConnection(database);
             PreparedStatement ps = conn.prepareStatement(trimmed)) {

            ps.setQueryTimeout(databaseService.getConfig().getQueryTimeoutMs() / 1000);
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
            }

            boolean isResultSet = ps.execute();
            long executionTimeMs = System.currentTimeMillis() - start;

            if (isResultSet) {
                try (ResultSet rs = ps.getResultSet()) {
                    ResultSetMetaData md = rs.getMetaData();
                    int colCount = md.getColumnCount();
                    List<String> columnNames = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        columnNames.add(md.getColumnLabel(i));
                    }

                    List<Map<String, Object>> rows = new ArrayList<>();
                    int count = 0;
                    boolean truncated = false;

                    while (rs.next()) {
                        if (count >= limit) {
                            truncated = true;
                            break;
                        }
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(columnNames.get(i - 1), rs.getObject(i));
                        }
                        rows.add(row);
                        count++;
                    }

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("type", "select");
                    result.put("rowCount", rows.size());
                    result.put("truncated", truncated);
                    result.put("maxRowsApplied", limit);
                    result.put("executionTimeMs", executionTimeMs);
                    result.put("columns", columnNames);
                    if ("markdown".equalsIgnoreCase(format)) {
                        result.put("markdown", TableFormatter.formatAsMarkdownTable(rows));
                    }
                    result.put("rows", rows);
                    return result;
                }
            } else {
                int affectedRows = ps.getUpdateCount();
                String insertId = null;
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        insertId = String.valueOf(generatedKeys.getObject(1));
                    }
                } catch (Exception ignored) {}

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("type", "mutation");
                result.put("executionTimeMs", executionTimeMs);
                result.put("affectedRows", affectedRows);
                result.put("insertId", insertId);
                result.put("message", "Query executed successfully. Affected rows: " + affectedRows);
                return result;
            }
        }
    }

    public Map<String, Object> explainQuery(String sql, Boolean analyze, String database) throws SQLException {
        String trimmed = sql.trim();
        String explainSql = (analyze != null && analyze) ? "EXPLAIN ANALYZE " + trimmed : "EXPLAIN FORMAT=JSON " + trimmed;

        try (Connection conn = databaseService.getConnection(database);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {

            List<String> lines = new ArrayList<>();
            while (rs.next()) {
                lines.add(rs.getString(1));
            }
            return Map.of(
                    "sql", trimmed,
                    "mode", (analyze != null && analyze) ? "EXPLAIN ANALYZE" : "EXPLAIN FORMAT=JSON",
                    "explainResult", String.join("\n", lines)
            );
        }
    }

    public Map<String, Object> executeTransaction(List<Map<String, Object>> statements, String database) throws SQLException {
        if (databaseService.getConfig().isReadOnly()) {
            throw new SecurityException("Database is in READ_ONLY mode. Transactions with potential writes are forbidden.");
        }
        if (statements == null || statements.isEmpty()) {
            throw new IllegalArgumentException("Statements list cannot be empty.");
        }

        long start = System.currentTimeMillis();
        List<Map<String, Object>> stepResults = new ArrayList<>();

        try (Connection conn = databaseService.getConnection(database)) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < statements.size(); i++) {
                    Map<String, Object> stmtObj = statements.get(i);
                    String sql = (String) stmtObj.get("sql");
                    @SuppressWarnings("unchecked")
                    List<Object> params = (List<Object>) stmtObj.get("params");

                    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        if (params != null) {
                            for (int p = 0; p < params.size(); p++) {
                                ps.setObject(p + 1, params.get(p));
                            }
                        }
                        int affected = ps.executeUpdate();
                        String insertId = null;
                        try (ResultSet gk = ps.getGeneratedKeys()) {
                            if (gk.next()) insertId = String.valueOf(gk.getObject(1));
                        }
                        stepResults.add(Map.of(
                                "stepIndex", i + 1,
                                "affectedRows", affected,
                                "insertId", insertId != null ? insertId : "null"
                        ));
                    }
                }
                conn.commit();
                return Map.of(
                        "success", true,
                        "statementCount", statements.size(),
                        "executionTimeMs", System.currentTimeMillis() - start,
                        "steps", stepResults
                );
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Transaction failed and was rolled back: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Map<String, Object> batchExecute(String sql, String database) throws SQLException {
        if (databaseService.getConfig().isReadOnly()) {
            throw new SecurityException("Database is in READ_ONLY mode. Batch execution is forbidden.");
        }

        long start = System.currentTimeMillis();
        try (Connection conn = databaseService.getConnection(database);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            long executionTimeMs = System.currentTimeMillis() - start;

            return Map.of(
                    "success", true,
                    "executionTimeMs", executionTimeMs,
                    "message", "Batch SQL script executed successfully"
            );
        }
    }
}
