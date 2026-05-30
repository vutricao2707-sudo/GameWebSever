package com.auction.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Railway + HikariCP Connection Pool
 */
public class DatabaseConnection {

    private static final String URL = ConfigLoader.getProperty("db.url");
    private static final String USER = ConfigLoader.getProperty("db.username");
    private static final String PASSWORD = ConfigLoader.getProperty("db.password");

    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();

            // 1. DATABASE INFO
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);

            // 2. RAILWAY OPTIMIZATION (Cấu hình chống sập)
            config.setMaximumPoolSize(5);        // Giới hạn 5 luồng để bảo vệ DB
            config.setMinimumIdle(1);            // Giữ ít nhất 1 luồng chờ sẵn
            config.setConnectionTimeout(10000);  // Giảm xuống 10s: Hết luồng thì báo lỗi luôn, không treo app
            config.setIdleTimeout(60000);        // Đóng luồng rảnh sau 60s
            config.setMaxLifetime(300000);       // Reset luồng sau 5 phút để tránh Railway cắt ngầm
            config.setKeepaliveTime(60000);      // Ping giữ mạng mỗi 60s
            config.setValidationTimeout(5000);

            // Cảnh báo nếu code quên đóng connection
            config.setLeakDetectionThreshold(60000);
            config.setPoolName("AuctionSystemPool");

            // 3. MYSQL OPTIMIZATION
            config.setConnectionInitSql("SET time_zone = '+07:00'");

            // Bật bộ nhớ đệm (Cache) để tăng tốc độ truy vấn liên tục
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");

            // 4. CREATE POOL
            dataSource = new HikariDataSource(config);
            System.out.println("✅ HikariCP connected to Railway! (Optimized Pool)");

        } catch (Exception e) {
            System.err.println("❌ Lỗi khởi tạo HikariCP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Lấy connection từ pool
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("HikariDataSource chưa được khởi tạo!");
        }
        return dataSource.getConnection();
    }

}