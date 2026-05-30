package com.auction.service;

import com.auction.database.DatabaseConnection;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionSyncManager — Đồng bộ thời gian cho toàn bộ hệ thống.
 */
public class AuctionSyncManager {

    private static final Logger logger = Logger.getLogger(AuctionSyncManager.class.getName());

    // Singleton
    private static volatile AuctionSyncManager instance;

    public static AuctionSyncManager getInstance() {
        if (instance == null) {
            synchronized (AuctionSyncManager.class) {
                if (instance == null) {
                    instance = new AuctionSyncManager();
                }
            }
        }
        return instance;
    }

    // Observer
    private final Map<Integer, List<AuctionListener>> listenerMap = new ConcurrentHashMap<>();

    // Scheduler chạy nền
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private AuctionSyncManager() {}


    //Interface mà các Controller phải implement để nhận sự kiện đồng bộ.

    public interface AuctionListener {

        //Gọi mỗi giây, cùng lúc trên tất cả màn hình đang lắng nghe.
        void onTimeTick(int itemId, long remainingSeconds, double currentPrice, String auctionStatus);

        //Gọi một lần duy nhất khi phiên chuyển sang trạng thái FINISHED.
        default void onAuctionEnded(int itemId, double finalPrice) {}

        //Gọi khi trạng thái chuyển từ REGISTRATION → AUCTION.
        default void onAuctionStarted(int itemId) {}
    }

    // QUẢN LÝ LISTENER

    //Đăng ký lắng nghe sự kiện cho một item cụ thể.

    public void addListener(int itemId, AuctionListener listener) {
        listenerMap.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>())
                .add(listener);
        logger.info("✅ [SyncManager] Listener added for itemId=" + itemId
                + " | Total items tracked: " + listenerMap.size());

        // Đảm bảo scheduler đang chạy
        ensureSchedulerRunning();
    }

    //Gỡ đăng ký listener. Nếu không còn ai theo dõi item đó
    public void removeListener(int itemId, AuctionListener listener) {
        List<AuctionListener> listeners = listenerMap.get(itemId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                listenerMap.remove(itemId);
                logger.info("🗑️ [SyncManager] itemId=" + itemId + " removed (no listeners left)");
            }
        }

        // Nếu không còn item nào → dừng scheduler để tiết kiệm CPU
        if (listenerMap.isEmpty()) {
            stopScheduler();
        }
    }

    //Xoá tất cả listener của một item d
    public void removeAllListeners(int itemId) {
        listenerMap.remove(itemId);
        logger.info("🗑️ [SyncManager] All listeners removed for itemId=" + itemId);
        if (listenerMap.isEmpty()) stopScheduler();
    }

    // VÒNG LẶP ĐỒNG BỘ CHÍNH
    private synchronized void ensureSchedulerRunning() {
        if (running.get()) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionSyncManager-Tick");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
        running.set(true);
        logger.info(" [SyncManager] Scheduler started");
    }

    private synchronized void stopScheduler() {
        if (!running.get()) return;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        running.set(false);
        logger.info("🛑 [SyncManager] Scheduler stopped (no active listeners)");
    }

    /**
     * Được gọi mỗi giây bởi scheduler.
     * Query DB lấy dữ liệu mới nhất, tính thời gian còn lại,
     * rồi notify tất cả listeners trên JavaFX Thread.
     */
    private void tick() {

        // Snapshot danh sách itemId để tránh ConcurrentModificationException
        if (listenerMap.isEmpty()) return;

        System.out.printf("⏱ [SyncManager] Tick — tracking %d item(s): %s%n",
                listenerMap.size(), listenerMap.keySet());

        for (Integer itemId : listenerMap.keySet()) {

            try {

                AuctionSnapshot snap = querySnapshot(itemId);

                if (snap == null) continue;

                // Tính thời gian còn lại
                long remaining = calculateRemaining(snap);

                // Status cũ từ DB
                String prevStatus = snap.auctionStatus;

                // Status mới sau khi resolve logic
                String resolvedStatus = resolveStatus(snap, remaining);

                // Nếu status thay đổi → update DB
                if (!resolvedStatus.equalsIgnoreCase(prevStatus)) {

                    updateAuctionStatus(itemId, resolvedStatus);

                    snap.auctionStatus = resolvedStatus;

                    logger.info("🔄 [SyncManager] itemId=" + itemId
                            + " status changed: "
                            + prevStatus + " -> "
                            + resolvedStatus);
                }

                // Data final cho lambda
                final long finalRemaining = remaining;
                final String finalStatus = resolvedStatus;
                final double finalPrice = snap.currentPrice;

                // Event detect
                final boolean justEnded =
                        "FINISHED".equals(resolvedStatus)
                                && !"FINISHED".equals(prevStatus)
                                && remaining <= 0;

                final boolean justStarted =
                        "AUCTION".equals(resolvedStatus)
                                && "REGISTRATION".equals(prevStatus);

                List<AuctionListener> listeners = listenerMap.get(itemId);

                if (listeners == null || listeners.isEmpty()) continue;

                // Update UI trên JavaFX Thread
                Platform.runLater(() -> {

                    for (AuctionListener l : listeners) {

                        try {

                            l.onTimeTick(
                                    itemId,
                                    finalRemaining,
                                    finalPrice,
                                    finalStatus
                            );

                            if (justEnded) {
                                l.onAuctionEnded(itemId, finalPrice);
                            }

                            if (justStarted) {
                                l.onAuctionStarted(itemId);
                            }

                        } catch (Exception e) {

                            logger.log(
                                    Level.WARNING,
                                    "[SyncManager] Listener error for itemId=" + itemId,
                                    e
                            );
                        }
                    }
                });

            } catch (Exception e) {

                logger.log(
                        Level.WARNING,
                        "[SyncManager] tick() error for itemId=" + itemId,
                        e
                );
            }
        }
    }

    // DATABASE QUERY

    /** Snapshot nhẹ từ DB — chỉ lấy đúng những cột cần thiết. */
    private AuctionSnapshot querySnapshot(int itemId) {
        String sql =
                "SELECT current_price, auction_status, " +
                        "       registration_end_time, auction_end_time " +
                        "FROM items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AuctionSnapshot snap = new AuctionSnapshot();
                    snap.itemId              = itemId;
                    snap.currentPrice        = rs.getDouble("current_price");
                    snap.auctionStatus       = rs.getString("auction_status");
                    snap.registrationEndTime = rs.getString("registration_end_time");
                    snap.auctionEndTime      = rs.getString("auction_end_time");
                    return snap;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[SyncManager] querySnapshot error itemId=" + itemId, e);
        }
        return null;
    }

    // ═════════════════════════════════════════════════════════════
    // LOGIC THỜI GIAN
    // ═════════════════════════════════════════════════════════════
    /**
     * Tính số giây còn lại dựa trên trạng thái hiện tại.
     * - REGISTRATION → đếm đến registration_end_time
     * - AUCTION      → đếm đến auction_end_time
     * - Còn lại      → 0
     */
    private long calculateRemaining(AuctionSnapshot snap) {
        String targetRaw;

        if ("REGISTRATION".equalsIgnoreCase(snap.auctionStatus)) {
            targetRaw = snap.registrationEndTime;
        } else if ("AUCTION".equalsIgnoreCase(snap.auctionStatus)) {
            targetRaw = snap.auctionEndTime;
        } else {
            return 0L;
        }

        if (targetRaw == null || targetRaw.isBlank()) return 0L;

        try {
            String cleaned = targetRaw.replace("T", " ");
            if (cleaned.contains(".")) cleaned = cleaned.substring(0, cleaned.indexOf('.'));

            // --- ÉP CHUẨN MÚI GIỜ VIỆT NAM ---
            LocalDateTime targetLocal = java.sql.Timestamp.valueOf(cleaned).toLocalDateTime();
            java.time.ZonedDateTime targetTime = targetLocal.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

            long secs = java.time.Duration.between(now, targetTime).getSeconds();
            // ---------------------------------

            return Math.max(secs, 0L);
        } catch (Exception e) {
            logger.log(Level.WARNING, "[SyncManager] parseTime error: " + targetRaw, e);
            return 0L;
        }
    }

    /**
     * Nếu đồng hồ về 0 mà DB vẫn chưa cập nhật, trả về trạng thái
     * logic đúng để UI hiển thị ngay — không phải chờ AuctionFinishService.
     */
    private String resolveStatus(AuctionSnapshot snap, long remaining) {

        String current =
                snap.auctionStatus == null
                        ? "WAITING"
                        : snap.auctionStatus.trim().toUpperCase();

        // REGISTRATION -> AUCTION
        if ("REGISTRATION".equals(current) && remaining <= 0) {
            return "AUCTION";
        }

        // AUCTION -> FINISHED
        if ("AUCTION".equals(current) && remaining <= 0) {
            return "FINISHED";
        }

        // FINISHED giữ nguyên
        if ("FINISHED".equals(current)) {
            return "FINISHED";
        }

        return current;
    }

    // ═════════════════════════════════════════════════════════════
    // TIỆN ÍCH CÔNG KHAI
    // ═════════════════════════════════════════════════════════════

    /** Dừng hẳn khi tắt ứng dụng (gọi từ App.java stage.setOnCloseRequest). */
    public void shutdown() {
        listenerMap.clear();
        stopScheduler();
        logger.info("🛑 [SyncManager] Shutdown complete");
    }

    /** Số item đang được theo dõi — hữu ích cho debug. */
    public int getTrackedItemCount() {
        return listenerMap.size();
    }

    // ═════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═════════════════════════════════════════════════════════════

    /** Dữ liệu thô lấy từ DB mỗi giây, không expose ra ngoài class. */
    private static class AuctionSnapshot {
        int    itemId;
        double currentPrice;
        String auctionStatus;
        String registrationEndTime;
        String auctionEndTime;
    }
    private void updateAuctionStatus(int itemId, String newStatus) {

        String sql = "UPDATE items SET auction_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, itemId);

            ps.executeUpdate();

        } catch (SQLException e) {

            logger.log(Level.WARNING,
                    "[SyncManager] updateAuctionStatus error itemId=" + itemId,
                    e);
        }
    }
}
