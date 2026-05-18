package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * WatchlistDAO - Quản lý danh sách theo dõi của người dùng
 * Users có thể theo dõi các biển số chưa mở đấu giá
 * Sẽ nhận thông báo tự động khi biển số vào giai đoạn đăng ký
 */
public class WatchlistDAO {

    /**
     * Thêm một item vào watchlist của user
     * @param userId ID của user
     * @param itemId ID của item (license plate)
     * @return true nếu thêm thành công, false nếu đã tồn tại hoặc lỗi
     */
    public static boolean addToWatchlist(int userId, int itemId) {
        String sql = "INSERT INTO watchlist (user_id, item_id, plate_number) " +
                "SELECT ?, ?, plate_number FROM items WHERE id = ? " +
                "ON DUPLICATE KEY UPDATE added_at = CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, itemId);
            pstmt.setInt(3, itemId);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("✅ Đã thêm item vào watchlist của user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi addToWatchlist: " + e.getMessage());
        }
        return false;
    }

    /**
     * Xóa một item từ watchlist của user
     * @param userId ID của user
     * @param itemId ID của item
     * @return true nếu xóa thành công
     */
    public static boolean removeFromWatchlist(int userId, int itemId) {
        String sql = "DELETE FROM watchlist WHERE user_id = ? AND item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, itemId);

            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("✅ Đã xóa item khỏi watchlist của user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi removeFromWatchlist: " + e.getMessage());
        }
        return false;
    }

    /**
     * Kiểm tra xem user có theo dõi item này không
     * @param userId ID của user
     * @param itemId ID của item
     * @return true nếu đang theo dõi
     */
    public static boolean isInWatchlist(int userId, int itemId) {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE user_id = ? AND item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi isInWatchlist: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy danh sách tất cả watchlist items của một user
     * @param userId ID của user
     * @return List các item trong watchlist ([ id, plate_number, status, auction_status, added_at ])
     */
    public static List<String[]> getUserWatchlist(int userId) {
        List<String[]> watchlist = new ArrayList<>();

        String sql = "SELECT w.id, w.item_id, i.plate_number, i.status, i.auction_status, " +
                "i.current_price, i.start_price, w.added_at, w.notification_sent " +
                "FROM watchlist w " +
                "JOIN items i ON w.item_id = i.id " +
                "WHERE w.user_id = ? " +
                "ORDER BY w.added_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    watchlist.add(new String[]{
                            rs.getString("id"),              // [0] watchlist id
                            rs.getString("item_id"),         // [1] item id
                            rs.getString("plate_number"),    // [2] biển số
                            rs.getString("status"),          // [3] status (PENDING, APPROVED, etc)
                            rs.getString("auction_status"),  // [4] auction status (WAITING, REGISTRATION, AUCTION, CLOSED)
                            rs.getString("current_price"),   // [5] giá hiện tại
                            rs.getString("start_price"),     // [6] giá khởi điểm
                            rs.getTimestamp("added_at").toString(), // [7] ngày thêm vào
                            rs.getString("notification_sent") // [8] đã gửi thông báo chưa
                    });
                }
            }

            System.out.println("✅ Đã tải " + watchlist.size() + " watchlist items cho user " + userId);

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getUserWatchlist: " + e.getMessage());
        }

        return watchlist;
    }

    /**
     * Lấy danh sách items trong watchlist đang ở trạng thái REGISTRATION
     * chưa được gửi thông báo
     * @param userId ID của user
     * @return List các item cần thông báo
     */
    public static List<String[]> getPendingNotifications(int userId) {
        List<String[]> pending = new ArrayList<>();

        String sql = "SELECT w.id, w.item_id, i.plate_number, i.vehicle_type, i.seller_name " +
                "FROM watchlist w " +
                "JOIN items i ON w.item_id = i.id " +
                "WHERE w.user_id = ? " +
                "AND w.notification_sent = FALSE " +
                "AND i.auction_status = 'REGISTRATION'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pending.add(new String[]{
                            rs.getString("id"),          // [0] watchlist id
                            rs.getString("item_id"),     // [1] item id
                            rs.getString("plate_number"), // [2] biển số
                            rs.getString("vehicle_type"), // [3] loại xe
                            rs.getString("seller_name")  // [4] tên người bán
                    });
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getPendingNotifications: " + e.getMessage());
        }

        return pending;
    }

    /**
     * Đánh dấu một thông báo là đã gửi
     * @param watchlistId ID của watchlist entry
     */
    public static void markNotificationAsSent(int watchlistId) {
        String sql = "UPDATE watchlist SET notification_sent = TRUE WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, watchlistId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("❌ Lỗi markNotificationAsSent: " + e.getMessage());
        }
    }

    /**
     * Xóa watchlist items khi item bị xóa từ hệ thống
     * @param itemId ID của item
     */
    public static void deleteWatchlistByItemId(int itemId) {
        String sql = "DELETE FROM watchlist WHERE item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("✅ Đã xóa " + rowsDeleted + " watchlist entries cho item " + itemId);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi deleteWatchlistByItemId: " + e.getMessage());
        }
    }

    /**
     * Lấy số lượng items trong watchlist
     * @param userId ID của user
     * @return số lượng items
     */
    public static int getWatchlistCount(int userId) {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getWatchlistCount: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Lấy danh sách users có theo dõi một item cụ thể
     * @param itemId ID của item
     * @return List các user IDs
     */
    public static List<Integer> getUsersWatchingItem(int itemId) {
        List<Integer> userIds = new ArrayList<>();

        String sql = "SELECT DISTINCT user_id FROM watchlist WHERE item_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getUsersWatchingItem: " + e.getMessage());
        }

        return userIds;
    }
}

