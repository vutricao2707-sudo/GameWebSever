package com.auction.service;

import com.auction.model.Watchlist;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WatchlistService - Quản lý watchlist của người dùng
 * Sử dụng in-memory storage (có thể kết nối với database sau)
 */
public class WatchlistService {
    private static final Logger logger = Logger.getLogger(WatchlistService.class.getName());
    private static List<Watchlist> watchlistMemory = new ArrayList<>();

    /**
     * Thêm biển số vào watchlist
     */
    public static boolean addToWatchlist(int userId, String plateNumber) {
        try {
            // Kiểm tra xem đã có chưa
            for (Watchlist w : watchlistMemory) {
                if (w.getUserId() == userId && w.getPlateNumber().equals(plateNumber)) {
                    logger.warning("⚠️ Watchlist đã tồn tại: userId=" + userId + ", plate=" + plateNumber);
                    return false;
                }
            }
            watchlistMemory.add(new Watchlist(userId, plateNumber));
            logger.info("✅ Thêm watchlist: userId=" + userId + ", plate=" + plateNumber);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi thêm watchlist", e);
            return false;
        }
    }

    /**
     * Xóa biển số khỏi watchlist
     */
    public static boolean removeFromWatchlist(int userId, String plateNumber) {
        try {
            boolean removed = watchlistMemory.removeIf(w -> 
                w.getUserId() == userId && w.getPlateNumber().equals(plateNumber)
            );
            if (removed) {
                logger.info("✅ Xóa watchlist: userId=" + userId + ", plate=" + plateNumber);
            } else {
                logger.warning("⚠️ Watchlist không tìm thấy");
            }
            return removed;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi xóa watchlist", e);
            return false;
        }
    }

    /**
     * Lấy danh sách watchlist của một người dùng
     */
    public static List<Watchlist> getUserWatchlist(int userId) {
        try {
            List<Watchlist> result = new ArrayList<>();
            for (Watchlist w : watchlistMemory) {
                if (w.getUserId() == userId) {
                    result.add(w);
                }
            }
            logger.info("✅ Lấy watchlist cho userId=" + userId + " (" + result.size() + " items)");
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi lấy watchlist", e);
            return new ArrayList<>();
        }
    }

    /**
     * Kiểm tra xem biển số đã trong watchlist không
     */
    public static boolean isInWatchlist(int userId, String plateNumber) {
        try {
            for (Watchlist w : watchlistMemory) {
                if (w.getUserId() == userId && w.getPlateNumber().equals(plateNumber)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi kiểm tra watchlist", e);
            return false;
        }
    }

    /**
     * Lấy số lượng watchlist của người dùng
     */
    public static int getWatchlistCount(int userId) {
        try {
            return (int) watchlistMemory.stream()
                    .filter(w -> w.getUserId() == userId)
                    .count();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi đếm watchlist", e);
            return 0;
        }
    }

    /**
     * Xóa tất cả watchlist
     */
    public static void clearAll() {
        watchlistMemory.clear();
        logger.info("✅ Xóa tất cả watchlist");
    }

    /**
     * Debug: In tất cả watchlist
     */
    public static void printAll() {
        logger.info("📋 Total watchlist items: " + watchlistMemory.size());
        for (Watchlist w : watchlistMemory) {
            logger.info(w.toString());
        }
    }
}

