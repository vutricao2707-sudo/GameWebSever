package com.auction.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Railway + HikariCP Connection Pool
 */
public class DatabaseConnection {

    private static final String URL =
            ConfigLoader.getProperty("db.url");

    private static final String USER =
            ConfigLoader.getProperty("db.username");

    private static final String PASSWORD =
            ConfigLoader.getProperty("db.password");

    private static HikariDataSource dataSource;

    static {

        HikariConfig config =
                new HikariConfig();

        // =========================
        // DATABASE INFO
        // =========================

        config.setJdbcUrl(URL);

        config.setUsername(USER);

        config.setPassword(PASSWORD);

        // =========================
        // RAILWAY OPTIMIZATION
        // =========================

        config.setMaximumPoolSize(5);

        config.setMinimumIdle(1);

        config.setConnectionTimeout(30000);

        // Railway kill idle connection khá nhanh
        config.setIdleTimeout(60000);

        // Refresh connection sớm hơn Railway timeout
        config.setMaxLifetime(300000);

        // Giữ connection luôn sống
        config.setKeepaliveTime(60000);

        config.setValidationTimeout(5000);

        // Detect leak
        config.setLeakDetectionThreshold(60000);

        config.setPoolName(
                "AuctionSystemPool"
        );

        // =========================
        // MYSQL OPTIMIZATION
        // =========================

        config.addDataSourceProperty(
                "cachePrepStmts",
                "true"
        );

        config.addDataSourceProperty(
                "prepStmtCacheSize",
                "250"
        );

        config.addDataSourceProperty(
                "prepStmtCacheSqlLimit",
                "2048"
        );

        config.addDataSourceProperty(
                "useServerPrepStmts",
                "true"
        );

        config.addDataSourceProperty(
                "rewriteBatchedStatements",
                "true"
        );

        config.addDataSourceProperty(
                "cacheResultSetMetadata",
                "true"
        );

        config.addDataSourceProperty(
                "cacheServerConfiguration",
                "true"
        );

        // =========================
        // CREATE POOL
        // =========================

        dataSource =
                new HikariDataSource(config);

        System.out.println(
                "✅ HikariCP connected to Railway!"
        );
    }

    /**
     * Lấy connection từ pool
     */
    public static Connection getConnection()
            throws SQLException {

        return dataSource.getConnection();
    }

    /**
     * Test connection
     */
    public static void testConnection() {

        try (Connection conn = getConnection()) {

            System.out.println(
                    "✅ Railway MySQL connected!"
            );

        } catch (SQLException e) {

            System.out.println(
                    "❌ DB Error: "
                            + e.getMessage()
            );
        }
    }
}