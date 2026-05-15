package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BidDAO - Quản lý lịch sử đặt giá (bids)
 */
public class BidDAO {

    // Thêm bid mới
    public static void addBid(int itemId, int userId, double amount) {

        String sql = "INSERT INTO bids (item_id, user_id, bid_amount) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            pstmt.setInt(2, userId);
            pstmt.setDouble(3, amount);

            pstmt.executeUpdate();

            System.out.println("✅ Đặt giá thành công");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi addBid: " + e.getMessage());
        }
    }


    // Lấy tất cả bids của 1 phiên (sắp xếp từ cao xuống thấp)
    public static void getBidsForAuction(int itemId) {

        String sql = "SELECT b.id, u.full_name, b.bid_amount, b.bid_time " +
                "FROM bids b " +
                "JOIN users u ON b.user_id = u.id " +
                "WHERE b.item_id = ? " +
                "ORDER BY b.bid_amount DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    System.out.println(
                            rs.getString("full_name") +
                                    " - " +
                                    rs.getDouble("bid_amount")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }


    // Lấy giá cao nhất của phiên
    public static double getHighestBidForAuction(int itemId) {

        String sql = "SELECT MAX(bid_amount) AS highest FROM bids WHERE item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getDouble("highest");
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return 0;
    }


    // Lấy người đặt giá cao nhất
    public static void getHighestBidder(int itemId) {

        String sql = "SELECT u.full_name, b.bid_amount " +
                "FROM bids b " +
                "JOIN users u ON b.user_id = u.id " +
                "WHERE b.item_id = ? " +
                "ORDER BY b.bid_amount DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    System.out.println(
                            "👑 " +
                                    rs.getString("full_name") +
                                    " - " +
                                    rs.getDouble("bid_amount")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }


    // Đếm số lần đặt giá trong phiên
    public static void countBidsForAuction(int itemId) {

        String sql = "SELECT COUNT(*) AS bid_count FROM bids WHERE item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    System.out.println(rs.getInt("bid_count"));
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    // ===== PHƯƠNG THỨC MỚI DÀNH CHO UI =====

    // Lấy tất cả bids của phiên (Return List để dùng cho UI)
    public static List<String[]> getAllBidsForAuction(int itemId) {

        List<String[]> bids = new ArrayList<>();

        String sql = "SELECT b.id, i.plate_number, b.bid_amount, b.bid_time " +
                "FROM bids b " +
                "JOIN items i ON b.item_id = i.id " +
                "WHERE b.item_id = ? " +
                "ORDER BY b.bid_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    bids.add(new String[]{
                            rs.getString("id"),
                            rs.getString("plate_number"),
                            String.valueOf(rs.getDouble("bid_amount")),
                            rs.getTimestamp("bid_time").toString()
                    });
                }
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
    // 8. Lưu bid khi bấm nút đặt giá - FIX CHUẨN
    public static void saveBid(String plateNumber, int userId, double bidAmount) {

        String sql = "INSERT INTO bids (item_id, user_id, bid_amount) " +
                "VALUES ((SELECT id FROM items WHERE plate_number = ?), ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setInt(2, userId);
            pstmt.setDouble(3, bidAmount);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("✅ Đã lưu lịch sử đặt giá cho biển: " + plateNumber);
            } else {
                System.out.println("❌ Không thể lưu lịch sử đặt giá");
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi saveBid: " + e.getMessage());
        }
    }
    // =====================================================================
    // LẤY LỊCH SỬ ĐẶT GIÁ CỦA RIÊNG USER ĐANG ĐĂNG NHẬP
    // =====================================================================
    // =====================================================================
    // LẤY LỊCH SỬ ĐẶT GIÁ CỦA RIÊNG USER (CÁ NHÂN HÓA)
    // =====================================================================
    public static List<String[]> getBidsByUserId(int userId) {
        List<String[]> bids = new ArrayList<>();

        // Câu lệnh SQL nối bảng bids và items để lấy tên biển số xe
        String sql = "SELECT b.id, i.plate_number, b.bid_amount, b.bid_time " +
                "FROM bids b " +
                "JOIN items i ON b.item_id = i.id " +
                "WHERE b.user_id = ? " +
                "ORDER BY b.bid_time DESC LIMIT 100";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            int stt = 1;
            while (rs.next()) {
                // Định dạng lại thời gian cho đẹp (bỏ phần .0 ở cuối nếu có)
                String rawTime = rs.getTimestamp("bid_time").toString();
                String formattedTime = rawTime.substring(0, Math.min(19, rawTime.length()));

                bids.add(new String[]{
                        String.valueOf(stt++),                  // [0] STT hiển thị
                        rs.getString("plate_number"),           // [1] Biển số
                        String.valueOf(rs.getDouble("bid_amount")), // [2] Giá tiền
                        formattedTime                           // [3] Thời gian đã format
                });
            }

            if (stt > 1) {
                System.out.println("✅ Đã tải " + (stt-1) + " dòng lịch sử cho User ID: " + userId);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi lấy lịch sử cá nhân: " + e.getMessage());
            // In stack trace để dễ debug nếu MySQL báo lỗi cột
            e.printStackTrace();
        }
        return bids;
    }
    // Kiểm tra lần đặt giá cuối cùng của user
    public static long getLastBidTime(String plateNumber, int userId) {

        String sql = "SELECT UNIX_TIMESTAMP(bid_time) " +
                "FROM bids " +
                "WHERE item_id = (SELECT id FROM items WHERE plate_number = ?) " +
                "AND user_id = ? " +
                "ORDER BY bid_time DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getLastBidTime: " + e.getMessage());
        }

        return 0;
    }
}