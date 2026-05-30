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

    // Hàm lưu lịch sử khi bấm đặt giá
    private static final long BID_COOLDOWN_SECONDS = 10; // 1 phút - cần match với client

    // Lưu bid khi bấm nút đặt giá
    public static synchronized boolean saveBid(String plateNumber, int userId, double bidAmount) {

        Connection conn = null;

        try {

            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. LOCK ROW ITEM
            String lockSql =
                    "SELECT id,\n" +
                            "       current_price,\n" +
                            "       auction_status,\n" +
                            "       auction_end_time " +
                            "FROM items " +
                            "WHERE plate_number = ? FOR UPDATE";

            int itemId;
            double currentPrice;
            String auctionStatus;
            Timestamp auctionEnd;

            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {

                lockStmt.setString(1, plateNumber);

                try (ResultSet rs = lockStmt.executeQuery()) {

                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    itemId = rs.getInt("id");
                    currentPrice = rs.getDouble("current_price");
                    auctionStatus = rs.getString("auction_status");
                    auctionEnd = rs.getTimestamp("auction_end_time");

                    if (auctionEnd == null ||
                            auctionEnd.before(new Timestamp(System.currentTimeMillis()))) {

                        System.out.println("❌ Phiên đã hết thời gian");

                        conn.rollback();
                        return false;
                    }
                }
            }

            // 2. CHECK AUCTION STATUS
            if (!"AUCTION".equalsIgnoreCase(auctionStatus)) {
                System.out.println("❌ Phiên chưa ở trạng thái AUCTION");
                conn.rollback();
                return false;
            }

            // 3. SERVER COOLDOWN
            long lastBidTime =
                    getLastBidTime(conn, plateNumber, userId);

            if (lastBidTime != 0) {

                long currentTime = System.currentTimeMillis() / 1000;
                long diff = currentTime - lastBidTime;

                if (diff < BID_COOLDOWN_SECONDS) {

                    System.out.println("⏳ Cooldown chưa hết");
                    conn.rollback();
                    return false;
                }
            }

            // 4. VALIDATE GIÁ
            if (bidAmount <= currentPrice) {

                System.out.println("❌ Giá bid phải lớn hơn giá hiện tại");
                conn.rollback();
                return false;
            }

            // 5. INSERT BID
            String insertSql =
                    "INSERT INTO bids (item_id, user_id, bid_amount) " +
                            "VALUES (?, ?, ?)";

            try (PreparedStatement insertStmt =
                         conn.prepareStatement(insertSql)) {

                insertStmt.setInt(1, itemId);
                insertStmt.setInt(2, userId);
                insertStmt.setDouble(3, bidAmount);

                insertStmt.executeUpdate();
            }

            // 6. UPDATE CURRENT PRICE
            String updateSql =
                    "UPDATE items " +
                            "SET current_price = ? " +
                            "WHERE id = ?";

            try (PreparedStatement updateStmt =
                         conn.prepareStatement(updateSql)) {

                updateStmt.setDouble(1, bidAmount);
                updateStmt.setInt(2, itemId);

                int updatedRows = updateStmt.executeUpdate();

                if (updatedRows == 0) {

                    System.out.println("❌ Không thể cập nhật giá");

                    conn.rollback();
                    return false;
                }
            }

            conn.commit();

            System.out.println("✅ BID SUCCESS");

            return true;

        } catch (Exception e) {

            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();
            return false;

        } finally {

            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
    private static long getLastBidTime(
            Connection conn,
            String plateNumber,
            int userId
    ) throws SQLException {

        String sql =
                "SELECT UNIX_TIMESTAMP(bid_time) " +
                        "FROM bids " +
                        "WHERE item_id = (" +
                        "SELECT id FROM items WHERE plate_number = ?" +
                        ") " +
                        "AND user_id = ? " +
                        "ORDER BY bid_time DESC LIMIT 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        return 0;
    }

    // AUTO-EXTEND TIMER
    public static boolean shouldExtendAuction(int itemId) {
        long timeRemaining = ItemDAO.getAuctionTimeRemainingSeconds(itemId);
        final long AUTO_EXTEND_THRESHOLD = 30; // 30s
        return timeRemaining > 0 && timeRemaining <= AUTO_EXTEND_THRESHOLD;
    }

    // PRICE HISTORY CHART - Real-time price trend visualization
    public static List<String[]> getBidHistoryForChart(int itemId) {
        List<String[]> bidHistory = new ArrayList<>();

        String sql = "SELECT b.id, b.bid_amount, b.bid_time, u.full_name " +
                "FROM bids b " +
                "JOIN users u ON b.user_id = u.id " +
                "WHERE b.item_id = ? " +
                "ORDER BY b.bid_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String formattedTime = rs.getTimestamp("bid_time").toString();
                    // Trim to just HH:mm:ss for display
                    if (formattedTime.length() > 8) {
                        formattedTime = formattedTime.substring(11, 19);
                    }

                    bidHistory.add(new String[]{
                            rs.getString("id"),
                            String.valueOf(rs.getDouble("bid_amount")),
                            formattedTime,
                            rs.getString("full_name")
                    });
                }
            }

            System.out.println("✅ Đã tải " + bidHistory.size() + " bids cho biểu đồ giá");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getBidHistoryForChart: " + e.getMessage());
        }

        return bidHistory;
    }

    //Lấy tổng số bids
    public static int getBidCount(int itemId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getBidCount: " + e.getMessage());
        }

        return 0;
    }

    //Kiểm tra lần đặt giá cuối cùng của user cho một item
    public static long getLastBidTimeForUser(int itemId, int userId) {
        String sql = "SELECT UNIX_TIMESTAMP(bid_time) " +
                "FROM bids " +
                "WHERE item_id = ? " +
                "AND user_id = ? " +
                "ORDER BY bid_time DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getLastBidTimeForUser: " + e.getMessage());
        }

        return 0;
    }

    //Lấy toàn bộ lịch sử đấu giá của 1 user
    public static List<String[]> getBidsByUserId(int userId) {

        List<String[]> bids = new ArrayList<>();

        String sql =
                "SELECT b.id, i.plate_number, b.bid_amount, b.bid_time " +
                        "FROM bids b " +
                        "JOIN items i ON b.item_id = i.id " +
                        "WHERE b.user_id = ? " +
                        "ORDER BY b.bid_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                int stt = 1;

                while (rs.next()) {

                    bids.add(new String[]{
                            String.valueOf(stt++),
                            rs.getString("plate_number"),
                            String.valueOf(rs.getDouble("bid_amount")),
                            rs.getTimestamp("bid_time").toString()
                    });
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getBidsByUserId: " + e.getMessage());
            e.printStackTrace();
        }

        return bids;
    }
}