package com.auction.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Kết nối MySQL - Dùng file database.properties
*/
public class DatabaseConnection {

    private static final String URL = ConfigLoader.getProperty("db.url");
    private static final String USER = ConfigLoader.getProperty("db.username");
    private static final String PASSWORD = ConfigLoader.getProperty("db.password");
    
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    
    // Gọi hàm này để test: DatabaseConnection.testConnection()
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Kết nối MySQL thành công!");
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}
