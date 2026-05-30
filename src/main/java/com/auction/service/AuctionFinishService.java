package com.auction.service;

import com.auction.server.AuctionWebSocketServer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionFinishService — Singleton chạy nền (JavaFX Timeline, 5 giây/lần).
 *
 * Trách nhiệm:
 *  1. Quét tất cả item có auction_status = 'AUCTION' và auction_end_time đã qua.
 *  2. Cập nhật DB: auction_status = 'FINISHED', status = 'INACTIVE'.
 *  3. Xác định winner (bid cao nhất) và tập hợp losers.
 *  4. Broadcast AUCTION_FINISHED qua WebSocket để tất cả controller lắng nghe.
 *  5. Thông báo kết quả vào bảng auction_results
 */

public class AuctionFinishService {

    private static final Logger logger = Logger.getLogger(AuctionFinishService.class.getName());
    private static AuctionFinishService instance;

    private Timeline pollingTimeline;
    private final List<AuctionResultListener> listeners = new CopyOnWriteArrayList<>();

    // LISTENER — Để AdminController, DashboardController đăng ký lắng nghe
    public interface AuctionResultListener {
        /**
         * Được gọi trên JavaFX Application Thread khi 1 phiên vừa kết thúc.
         * @param plateNumber biển số vừa kết thúc
         * @param finalPrice  giá cuối cùng
         * @param winnerId    ID người thắng (0 nếu không có ai đặt giá)
         * @param winnerName  Tên người thắng
         */
        void onAuctionFinished(String plateNumber, double finalPrice, int winnerId, String winnerName);
    }

    // SINGLETON
    private AuctionFinishService() {}

    public static synchronized AuctionFinishService getInstance() {
        if (instance == null) {
            instance = new AuctionFinishService();
        }
        return instance;
    }

    // KHỞI ĐỘNG / DỪNG

    /** Gọi một lần trong App.java sau khi JavaFX Application Thread sẵn sàng. */
    public void start() {
        if (pollingTimeline != null) return; // Đã chạy rồi

        pollingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> checkAndFinishExpiredAuctions())
        );
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
        logger.info("✅ AuctionFinishService started (polling every 5s)");
    }

    public void stop() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }

    // ĐĂNG KÝ / HỦY LẮNG NGHE
    public void addListener(AuctionResultListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(AuctionResultListener l) {
        listeners.remove(l);
    }

    // LOGIC KIỂM TRA VÀ KẾT THÚC PHIÊN

    private void checkAndFinishExpiredAuctions() {
        Thread worker = new Thread(() -> {
            try {
                List<ExpiredAuction> expiredList = queryExpiredAuctions();
                for (ExpiredAuction expired : expiredList) {
                    processFinishAuction(expired);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Lỗi trong checkAndFinishExpiredAuctions", e);
            }
        }, "AuctionFinish-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Query tất cả phiên đã hết giờ nhưng chưa được đánh dấu FINISHED.
     */
    private List<ExpiredAuction> queryExpiredAuctions() {
        List<ExpiredAuction> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, current_price " +
                "FROM items " +
                "WHERE auction_status = 'AUCTION' " +
                "  AND auction_end_time IS NOT NULL " +
                "  AND auction_end_time <= NOW()";

        try (Connection conn = com.auction.database.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new ExpiredAuction(
                        rs.getInt("id"),
                        rs.getString("plate_number"),
                        rs.getDouble("current_price")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi query expired auctions", e);
        }
        return result;
    }

    /**
     * Xử lý kết thúc cho 1 phiên: cập nhật DB, tìm winner, broadcast.
     */
    private void processFinishAuction(ExpiredAuction expired) {
        // 1. Đánh dấu FINISHED trong DB (atomic — tránh race condition)
        boolean finished = markAsFinished(expired.itemId, expired.currentPrice);
        if (!finished) return; // Đã bị xử lý bởi thread khác

        logger.info("🏁 Phiên kết thúc: " + expired.plateNumber + " | Giá: " + expired.currentPrice);

        // 2. Tìm winner (bid cao nhất)
        WinnerInfo winner = findWinner(expired.itemId);

        int winnerId = winner != null ? winner.userId : 0;
        String winnerName = winner != null ? winner.fullName : "Không có người đặt giá";
        double finalPrice = winner != null ? winner.bidAmount : expired.currentPrice;

        // 3. Lưu kết quả vào bảng auction_results (tùy chọn)
        saveAuctionResult(expired.itemId, winnerId, finalPrice);

        // 4. Broadcast qua WebSocket
        try {
            AuctionWebSocketServer wsServer = AuctionWebSocketServer.getInstance();
            wsServer.broadcastAuctionFinished(expired.itemId, finalPrice, winnerId, winnerName);
            logger.info("📡 Broadcast AUCTION_FINISHED: itemId=" + expired.itemId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Lỗi broadcast WebSocket", e);
        }

        // 5. Notify listeners trên JavaFX thread
        final String plateNumber = expired.plateNumber;
        final double fp = finalPrice;
        final int wId = winnerId;
        final String wName = winnerName;

        Platform.runLater(() -> {
            for (AuctionResultListener listener : listeners) {
                try {
                    listener.onAuctionFinished(plateNumber, fp, wId, wName);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Lỗi listener.onAuctionFinished", e);
                }
            }
        });
    }

    /**
     * Dùng UPDATE với điều kiện auction_status = 'AUCTION' để đảm bảo atomic.
     * Trả về true nếu chính thread này đã finish thành công (không bị race condition).
     */
    private boolean markAsFinished(int itemId, double finalPrice) {
        String sql = "UPDATE items " +
                "SET auction_status = 'FINISHED', " +
                "    current_price = ? " +
                "WHERE id = ? AND auction_status = 'AUCTION'";

        try (Connection conn = com.auction.database.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, finalPrice);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi markAsFinished itemId=" + itemId, e);
            return false;
        }
    }

    /**
     * Tìm người đặt giá cao nhất cho item.
     */
    private WinnerInfo findWinner(int itemId) {
        String sql = "SELECT b.user_id, b.bid_amount, u.full_name " +
                "FROM bids b " +
                "JOIN users u ON b.user_id = u.id " +
                "WHERE b.item_id = ? " +
                "ORDER BY b.bid_amount DESC LIMIT 1";
        try (Connection conn = com.auction.database.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new WinnerInfo(
                            rs.getInt("user_id"),
                            rs.getString("full_name"),
                            rs.getDouble("bid_amount")
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi findWinner itemId=" + itemId, e);
        }
        return null;
    }

    /**
     * Lưu kết quả vào bảng auction_results nếu tồn tại.
     */
    private void saveAuctionResult(int itemId, int winnerId, double finalPrice) {
        String sql = "INSERT IGNORE INTO auction_results (item_id, winner_id, final_price) " +
                "VALUES (?, ?, ?)";
        try (Connection conn = com.auction.database.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            if (winnerId > 0) ps.setInt(2, winnerId); else ps.setNull(2, Types.INTEGER);
            ps.setDouble(3, finalPrice);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Bảng có thể chưa tồn tại — không crash
            logger.log(Level.FINE, "saveAuctionResult skipped (table may not exist): " + e.getMessage());
        }
    }

    // HELPER CLASSES
    private static class ExpiredAuction {
        final int itemId;
        final String plateNumber;
        final double currentPrice;
        ExpiredAuction(int itemId, String plateNumber, double currentPrice) {
            this.itemId = itemId;
            this.plateNumber = plateNumber;
            this.currentPrice = currentPrice;
        }
    }

    private static class WinnerInfo {
        final int userId;
        final String fullName;
        final double bidAmount;
        WinnerInfo(int userId, String fullName, double bidAmount) {
            this.userId = userId;
            this.fullName = fullName;
            this.bidAmount = bidAmount;
        }
    }
}
