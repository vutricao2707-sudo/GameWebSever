package com.auction.database;

import java.sql.*;

/**
 * AuctionDAO - Quản lý phiên đấu giá (auctions)
 */
public class AuctionDAO {
    
    // Tạo phiên đấu giá mới
    public static void createAuction(int itemId, int sellerId) {
        String sql = "INSERT INTO auctions (item_id, seller_id, is_open) VALUES (?, ?, TRUE)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, sellerId);
            
            pstmt.executeUpdate();
            System.out.println("✅ Tạo phiên đấu giá thành công");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Lấy tất cả phiên đấu giá đang mở
    public static void getOpenAuctions() {
        String sql = "SELECT a.id, i.plate_number, i.current_price, u.full_name " +
                     "FROM auctions a " +
                     "JOIN items i ON a.item_id = i.id " +
                     "JOIN users u ON a.seller_id = u.id " +
                     "WHERE a.is_open = TRUE";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n📌 Phiên đấu giá đang mở:");
            while (rs.next()) {
                System.out.println("  Phiên #" + rs.getInt("id") + 
                                 " | Biển: " + rs.getString("plate_number") +
                                 " | Giá: " + rs.getDouble("current_price") +
                                 " | Seller: " + rs.getString("full_name"));
            }
            System.out.println();
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Đóng phiên đấu giá (khi hết giờ)
    public static void closeAuction(int auctionId, int winnerId, double finalPrice) {
        String sql = "UPDATE auctions SET is_open = FALSE, winner_id = ?, final_price = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, winnerId);
            pstmt.setDouble(2, finalPrice);
            pstmt.setInt(3, auctionId);
            
            pstmt.executeUpdate();
            System.out.println("✅ Đóng phiên đấu giá thành công");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Lấy chi tiết phiên đấu giá
    public static void getAuctionDetails(int auctionId) {
        String sql = "SELECT a.*, i.plate_number, u.full_name FROM auctions a " +
                     "JOIN items i ON a.item_id = i.id " +
                     "JOIN users u ON a.seller_id = u.id " +
                     "WHERE a.id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("\n📌 Chi tiết phiên:");
                System.out.println("  Phiên #: " + rs.getInt("id"));
                System.out.println("  Biển: " + rs.getString("plate_number"));
                System.out.println("  Người bán: " + rs.getString("full_name"));
                System.out.println("  Giá hiện tại: " + rs.getDouble("final_price"));
                System.out.println("  Trạng thái: " + (rs.getBoolean("is_open") ? "Đang mở" : "Đã đóng") + "\n");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}

