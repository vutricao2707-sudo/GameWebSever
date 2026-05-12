package com.auction.database;

import java.sql.*;

/**
 * ItemDAO - Quản lý biển số xe (items)
 */
public class ItemDAO {
    
    // Thêm item (biển số xe) mới
    public static void addItem(String plateNumber, double startPrice, String description, 
                               String vehicleType, String status) {
        String sql = "INSERT INTO items (plate_number, start_price, current_price, description, vehicle_type, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            pstmt.setDouble(2, startPrice);
            pstmt.setDouble(3, startPrice);  // current_price = start_price lúc đầu
            pstmt.setString(4, description);
            pstmt.setString(5, vehicleType);
            pstmt.setString(6, status);
            
            pstmt.executeUpdate();
            System.out.println("✅ Thêm item biển '" + plateNumber + "' thành công");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Lấy item theo plate_number
    public static void getItemByPlate(String plateNumber) {
        String sql = "SELECT * FROM items WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("\n📌 Thông tin item:");
                System.out.println("  ID: " + rs.getInt("id"));
                System.out.println("  Biển: " + rs.getString("plate_number"));
                System.out.println("  Giá khởi điểm: " + rs.getDouble("start_price"));
                System.out.println("  Giá hiện tại: " + rs.getDouble("current_price"));
                System.out.println("  Loại xe: " + rs.getString("vehicle_type"));
                System.out.println("  Trạng thái: " + rs.getString("status") + "\n");
            } else {
                System.out.println("❌ Không tìm thấy item");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Lấy tất cả items
    public static void getAllItems() {
        String sql = "SELECT plate_number, start_price, current_price, vehicle_type, status FROM items";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n📌 Danh sách items:");
            while (rs.next()) {
                System.out.println("  Biển: " + rs.getString("plate_number") + 
                                 " | Giá: " + rs.getDouble("current_price") + 
                                 " | Loại: " + rs.getString("vehicle_type"));
            }
            System.out.println();
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Cập nhật giá hiện tại của item
    public static void updateCurrentPrice(String plateNumber, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, plateNumber);
            
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Cập nhật giá thành công");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}

