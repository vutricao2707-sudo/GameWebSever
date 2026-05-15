package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    // ===== PHƯƠNG THỨC MỚI DÀNH CHO UI =====

    // Lấy tất cả bids của phiên (Return List để dùng cho UI)
    public static List<String[]> getAllBidsForAuction(int auctionId) {
        List<String[]> bids = new ArrayList<>();
        String sql = "SELECT b.id, u.full_name, b.amount, b.bid_time, i.plate_number " +
                "FROM bids b " +
                "JOIN users u ON b.bidder_id = u.id " +
                "JOIN auctions a ON b.auction_id = a.id " +
                "JOIN items i ON a.item_id = i.id " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.bid_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bids.add(new String[]{
                        rs.getString("id"),
                        rs.getString("plate_number"),
                        String.valueOf(rs.getDouble("amount")),
                        rs.getTimestamp("bid_time").toString(),
                        String.valueOf(rs.getInt("id")) // STT
                });
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return bids;
    }

    // Lấy tất cả bids (không cần auction_id)
    public static List<String[]> getAllBids() {
        List<String[]> bids = new ArrayList<>();

        // 1. Sửa câu truy vấn: Bỏ bảng auctions, dùng user_id và bid_amount
        String sql = "SELECT b.id, u.full_name, b.bid_amount, b.bid_time, i.plate_number " +
                "FROM bids b " +
                "JOIN users u ON b.user_id = u.id " +
                "JOIN items i ON b.item_id = i.id " +
                "ORDER BY b.bid_time DESC LIMIT 100";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            int stt = 1;
            while (rs.next()) {
                bids.add(new String[]{
                        String.valueOf(stt++),
                        rs.getString("plate_number"),

                        // 2. Lấy dữ liệu từ cột bid_amount thay vì amount
                        String.valueOf(rs.getDouble("bid_amount")),

                        rs.getTimestamp("bid_time").toString()
                });
            }

        } catch (SQLException e) {
            // In ra lỗi chi tiết nếu có để dễ debug
            System.out.println("❌ Lỗi tải lịch sử đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
        return bids;
    }

    // =====================================================================
    // HÀM LƯU LỊCH SỬ KHI BẤM "ĐẶT GIÁ" (Hỗ trợ cả 2 phiên bản Database)
    // =====================================================================
    public static void saveBid(String plateNumber, int userId, double bidAmount) {
        // Kịch bản 1: Cố gắng lưu theo chuẩn cũ (bảng auctions)
        String sqlOld = "INSERT INTO bids (auction_id, bidder_id, amount) " +
                "VALUES ((SELECT a.id FROM auctions a JOIN items i ON a.item_id = i.id WHERE i.plate_number = ? LIMIT 1), ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlOld)) {

            pstmt.setString(1, plateNumber);
            pstmt.setInt(2, userId);
            pstmt.setDouble(3, bidAmount);
            pstmt.executeUpdate();
            System.out.println("✅ Đã lưu lịch sử (Kịch bản 1 - Bảng cũ) cho biển: " + plateNumber);

        } catch (SQLException e) {
            // Kịch bản 2 (Dự phòng): Nếu Database của bạn đã cập nhật theo cấu trúc mới (không có bảng auctions)
            String sqlNew = "INSERT INTO bids (item_id, user_id, bid_amount) " +
                    "VALUES ((SELECT id FROM items WHERE plate_number = ?), ?, ?)";
            try (Connection conn2 = DatabaseConnection.getConnection();
                 PreparedStatement pstmt2 = conn2.prepareStatement(sqlNew)) {

                pstmt2.setString(1, plateNumber);
                pstmt2.setInt(2, userId);
                pstmt2.setDouble(3, bidAmount);
                pstmt2.executeUpdate();
                System.out.println("✅ Đã lưu lịch sử (Kịch bản 2 - Bảng mới) cho biển: " + plateNumber);

            } catch (SQLException ex) {
                System.out.println("❌ Lỗi lưu lịch sử cả 2 kịch bản: " + ex.getMessage());
            }
        }
    }
}