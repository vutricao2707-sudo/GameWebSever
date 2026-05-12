package com.auction.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Kết nối MySQL - File này càng đơn giản càng tốt
 * 
 * Các bạn chỉ cần:
 * 1. Thay PASSWORD thành mật khẩu MySQL của mình
 * 2. Chạy DatabaseInitializer.initDatabase() 1 lần khi app start
 * 3. Xong! Dùng UserDAO, ItemDAO, v.v. để query
 */
public class DatabaseConnection {
    
    // === CÁC BẠN ĐỔI ĐỦ ĐIỀU NÀY ===
    private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";  // <- ĐỔI TẠI ĐÂY theo mật khẩu MySQL của bạn
    
    
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
