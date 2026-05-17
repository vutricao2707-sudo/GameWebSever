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

        HikariConfig config = new HikariConfig();

        // =========================
        // DATABASE INFO
        // =========================
        config.setJdbcUrl(URL);

        config.setUsername(USER);

        config.setPassword(PASSWORD);

        // =========================
        // RAILWAY OPTIMIZATION
        // =========================

        // Railway free tier không nên quá nhiều
        config.setMaximumPoolSize(5);

        // Giữ ít connection idle
        config.setMinimumIdle(1);

        // Timeout lấy connection
        config.setConnectionTimeout(30000);

        // Idle timeout
        config.setIdleTimeout(600000);

        // Max lifetime trước khi refresh
        config.setMaxLifetime(1800000);

        // Test connection trước khi dùng
        config.setValidationTimeout(5000);

        // Tăng ổn định
        config.setLeakDetectionThreshold(60000);

        // Tên pool
        config.setPoolName("AuctionSystemPool");

        // =========================
        // MYSQL OPTIMIZATION
        // =========================
        config.addDataSourceProperty(
                "cachePrepStmts", "true"
        );

        config.addDataSourceProperty(
                "prepStmtCacheSize", "250"
        );

        config.addDataSourceProperty(
                "prepStmtCacheSqlLimit", "2048"
        );

        config.addDataSourceProperty(
                "useServerPrepStmts", "true"
        );

        // =========================
        // CREATE POOL
        // =========================
        dataSource = new HikariDataSource(config);

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
                    "❌ DB Error: " + e.getMessage()
            );
        }
    }
}