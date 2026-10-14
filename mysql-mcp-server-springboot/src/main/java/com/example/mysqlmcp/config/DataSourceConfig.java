package com.example.mysqlmcp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private final MysqlMcpConfig config;

    public DataSourceConfig(MysqlMcpConfig config) {
        this.config = config;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig hikari = new HikariConfig();
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setJdbcUrl(config.buildJdbcUrl(config.getDatabase()));
        hikari.setUsername(config.getUser());
        hikari.setPassword(config.getPassword());
        hikari.setMaximumPoolSize(config.getPoolLimit());
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(config.getQueryTimeoutMs());
        hikari.setIdleTimeout(600000);
        hikari.setMaxLifetime(1800000);
        hikari.setPoolName("MysqlMcpHikariPool");

        // Useful performance optimizations
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(hikari);
    }
}
