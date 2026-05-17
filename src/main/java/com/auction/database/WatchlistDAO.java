package com.auction.database;

import com.auction.model.Watchlist;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WatchlistDAO - Quản lý watchlist trong database
 * Cung cấp các phương thức CRUD cho bảng watchlist
 */
public class WatchlistDAO {
    private static final Logger logger = Logger.getLogger(WatchlistDAO.class.getName());

    /**
     * Thêm biển số vào watchlist
     */
    public static boolean addToWatchlist(int userId, String plateNumber) {
        String sql = "INSERT INTO watchlist (user_id, plate_number, added_date, notified) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, plateNumber);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setBoolean(4, false);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                logger.info("✅ Thêm vào watchlist: userId=" + userId + ", plate=" + plateNumber);
            }
            return rows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Lỗi thêm vào watchlist", e);
            return false;
        }
    }

    /**
     * Xóa biển số khỏi watchlist
     */
    public static boolean removeFromWatchlist(int userId, String plateNumber) {
        String sql = "DELETE FROM watchlist WHERE user_id = ? AND plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, plateNumber);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                logger.info("✅ Xóa khỏi watchlist: userId=" + userId + ", plate=" + plateNumber);
            }
            return rows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Lỗi xóa khỏi watchlist", e);
            return false;
        }
    }

    /**
     * Lấy danh sách watchlist của một người dùng
     */
    public static List<Watchlist> getUserWatchlist(int userId) {
        List<Watchlist> watchlist = new ArrayList<>();
        String sql = "SELECT id, user_id, plate_number, added_date, notified FROM watchlist WHERE user_id = ? ORDER BY added_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Watchlist w = new Watchlist(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("plate_number"),
                        rs.getTimestamp("added_date").toLocalDateTime(),
                        rs.getBoolean("notified")
                    );
                    watchlist.add(w);
                }
                logger.info("✅ Lấy watchlist cho userId=" + userId + " (" + watchlist.size() + " items)");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Lỗi lấy watchlist", e);
        }
        
        return watchlist;
    }

    /**
     * Kiểm tra xem biển số đã trong watchlist không
     */
    public static boolean isInWatchlist(int userId, String plateNumber) {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE user_id = ? AND plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, plateNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Lỗi kiểm tra watchlist", e);
        }
        
        return false;
    }

    /**
     * Đánh dấu watchlist đã gửi thông báo
     */
    public static boolean markAsNotified(int watchlistId) {
        String sql = "UPDATE watchlist SET notified = true WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, watchlistId);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                logger.info("✅ Đánh dấu watchlist đã thông báo: id=" + watchlistId);
            }
            return rows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Lỗi cập nhật watchlist", e);
            return false;
        }
    }

    /**
     * Lấy tất cả watchlist chưa gửi thông báo cho một biển số
     * Dùng để gửi email khi biển số được duyệt
     */
    public static List<Watchlist> getUnnotifiedWatchlistForPlate(String plateNumber) {
        List<Watchlist> watchlist = new ArrayList<>();
        String sql = "SELECT id, user_id, plate_number, added_date, notified FROM watchlist WHERE plate_number = ? AND notified = false";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Watchlist w = new Watchlist(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("plate_number"),
                        rs.getTimestamp("added_date").toLocalDateTime(),
                        rs.getBoolean("notified")
                    );
                    watchlist.add(w);
                }
                logger.info("✅ Lấy watchlist chưa thông báo cho plate=" + plateNumber + " (" + watchlist.size() + " items)");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Lỗi lấy watchlist chưa thông báo", e);
        }
        
        return watchlist;
    }

    /**
     * Xóa watchlist khi biển số đã kết thúc
     */
    public static boolean clearWatchlistForPlate(String plateNumber) {
        String sql = "DELETE FROM watchlist WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                logger.info("✅ Xóa watchlist cho plate=" + plateNumber + " (" + rows + " rows)");
            }
            return rows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Lỗi xóa watchlist cho plate", e);
            return false;
        }
    }

    /**
     * Lấy số lượng watchlist của người dùng
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
            logger.log(Level.SEVERE, "❌ Lỗi đếm watchlist", e);
        }
        return 0;
    }
}

