package com.example.mysqlmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mysql")
public class MysqlMcpConfig {
    private String host = "127.0.0.1";
    private int port = 3305;
    private String user = "root";
    private String password = "root";
    private String database = "dabba";
    private int poolLimit = 10;
    private int maxRows = 1000;
    private int queryTimeoutMs = 30000;
    private boolean readOnly = false;

    // Getters and Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public int getPoolLimit() { return poolLimit; }
    public void setPoolLimit(int poolLimit) { this.poolLimit = poolLimit; }

    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }

    public int getQueryTimeoutMs() { return queryTimeoutMs; }
    public void setQueryTimeoutMs(int queryTimeoutMs) { this.queryTimeoutMs = queryTimeoutMs; }

    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

    public String buildJdbcUrl(String targetDatabase) {
        String db = (targetDatabase != null && !targetDatabase.isBlank()) ? targetDatabase : (this.database != null ? this.database : "");
        return String.format("jdbc:mysql://%s:%d/%s?allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000",
                this.host, this.port, db);
    }
}
