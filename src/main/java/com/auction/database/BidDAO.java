package com.auction.database;

import java.sql.*;

/**
 * BidDAO - Quản lý lịch sử đặt giá (bids)
 */
public class BidDAO {
    
    // Thêm bid mới
    public static void addBid(int auctionId, int bidderId, double amount) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, amount);
            
            pstmt.executeUpdate();
            System.out.println("✅ Đặt giá " + amount + " thành công");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Lấy tất cả bids của 1 phiên (sắp xếp từ cao xuống thấp)
    public static void getBidsForAuction(int auctionId) {
        String sql = "SELECT b.id, u.full_name, b.amount, b.bid_time " +
                     "FROM bids b " +
                     "JOIN users u ON b.bidder_id = u.id " +
                     "WHERE b.auction_id = ? " +
                     "ORDER BY b.amount DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n📌 Lịch sử đặt giá (Phiên #" + auctionId + "):");
            while (rs.next()) {
                System.out.println("  " + rs.getString("full_name") + 
                                 " - " + rs.getDouble("amount") + 
                                 " đ (" + rs.getTimestamp("bid_time") + ")");
            }
            System.out.println();
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Lấy giá cao nhất của phiên
    public static double getHighestBidForAuction(int auctionId) {
        String sql = "SELECT MAX(amount) AS highest FROM bids WHERE auction_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("highest");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        
        return 0;
    }
    
    
    // Lấy người đặt giá cao nhất
    public static void getHighestBidder(int auctionId) {
        String sql = "SELECT u.full_name, b.amount " +
                     "FROM bids b " +
                     "JOIN users u ON b.bidder_id = u.id " +
                     "WHERE b.auction_id = ? " +
                     "ORDER BY b.amount DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("👑 Người đặt giá cao nhất: " + rs.getString("full_name") + 
                                 " (" + rs.getDouble("amount") + " đ)");
            } else {
                System.out.println("Chưa có ai đặt giá");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    
    // Đếm số lần đặt giá trong phiên
    public static void countBidsForAuction(int auctionId) {
        String sql = "SELECT COUNT(*) AS bid_count FROM bids WHERE auction_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("📊 Tổng lần đặt giá: " + rs.getInt("bid_count"));
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}

