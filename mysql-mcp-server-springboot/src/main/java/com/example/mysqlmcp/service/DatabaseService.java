package com.example.mysqlmcp.service;

import com.example.mysqlmcp.config.MysqlMcpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Service
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final DataSource dataSource;
    private final MysqlMcpConfig config;

    public DatabaseService(DataSource dataSource, MysqlMcpConfig config) {
        this.dataSource = dataSource;
        this.config = config;
    }

    public Connection getConnection(String optionalDatabase) throws SQLException {
        Connection conn = dataSource.getConnection();
        if (optionalDatabase != null && !optionalDatabase.isBlank()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("USE `" + optionalDatabase.replace("`", "``") + "`");
            }
        } else if (config.getDatabase() != null && !config.getDatabase().isBlank()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("USE `" + config.getDatabase().replace("`", "``") + "`");
            }
        }
        return conn;
    }

    public Map<String, Object> ping() {
        long start = System.currentTimeMillis();
        Map<String, Object> res = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.setQueryTimeout(5);
            ResultSet rs = stmt.executeQuery("SELECT 1 AS ping, VERSION() AS version, CURRENT_USER() AS user");
            if (rs.next()) {
                long latency = System.currentTimeMillis() - start;
                res.put("status", "healthy");
                res.put("latencyMs", latency);
                res.put("mysqlVersion", rs.getString("version"));
                res.put("currentUser", rs.getString("user"));
                res.put("host", config.getHost() + ":" + config.getPort());
                res.put("readOnly", config.isReadOnly());
            }
        } catch (Exception e) {
            log.error("MySQL Ping failed: {}", e.getMessage());
            res.put("status", "unhealthy");
            res.put("error", e.getMessage());
        }
        return res;
    }

    public MysqlMcpConfig getConfig() {
        return config;
    }
}
