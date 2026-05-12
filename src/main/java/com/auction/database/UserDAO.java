package com.auction.database;

import java.sql.*;

/**
 * DAO = Data Access Object
 * 
 * File này chứa các hàm để query bảng users
 * Ví dụ: thêm user, tìm user, lấy tất cả users, v.v.
 */
public class UserDAO {
    
    // Thêm user mới
    public static void addUser(String username, String password, String email, 
                               String fullName, String userType, String phone, String address) {
        String sql = "INSERT INTO users (username, password, email, full_name, user_type, phone, address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            pstmt.setString(4, fullName);
            pstmt.setString(5, userType);
            pstmt.setString(6, phone);
            pstmt.setString(7, address);
            
            pstmt.executeUpdate();
            System.out.println("✅ Thêm user '" + username + "' thành công");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Tìm user theo username
    public static void findUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("\n📌 Thông tin user:");
                System.out.println("  ID: " + rs.getInt("id"));
                System.out.println("  Username: " + rs.getString("username"));
                System.out.println("  Email: " + rs.getString("email"));
                System.out.println("  Tên: " + rs.getString("full_name"));
                System.out.println("  Loại: " + rs.getString("user_type"));
                System.out.println("  SĐT: " + rs.getString("phone") + "\n");
            } else {
                System.out.println("❌ Không tìm thấy user '" + username + "'");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Lấy tất cả users
    public static void getAllUsers() {
        String sql = "SELECT id, username, email, full_name, user_type FROM users";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n📌 Danh sách users:");
            while (rs.next()) {
                System.out.println("  " + rs.getInt("id") + ". " + rs.getString("username") + 
                                 " (" + rs.getString("user_type") + ")");
            }
            System.out.println();
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Xóa user
    public static void deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            int rowsDeleted = pstmt.executeUpdate();
            
            if (rowsDeleted > 0) {
                System.out.println("✅ Xóa user '" + username + "' thành công");
            } else {
                System.out.println("❌ Không tìm thấy user '" + username + "'");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Kiểm tra đăng nhập
    public static boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next();  // True nếu tìm thấy, False nếu không
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return false;
        }
    }
}

