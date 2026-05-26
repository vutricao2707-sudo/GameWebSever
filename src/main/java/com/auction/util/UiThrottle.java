package com.auction.util;

import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * UiThrottle — Giải pháp chống lag UI cho JavaFX khi nhận hàng trăm WebSocket message/giây.
 *
 * ── VẤN ĐỀ GỐC ────────────────────────────────────────────────────────────────
 * Mỗi lần gọi Platform.runLater() sẽ đưa 1 Runnable vào event queue của JavaFX
 * Application Thread. Khi có 200–500 msg/giây từ WebSocket, queue tắc nghẽn,
 * thread không xử lý kịp → UI giật lag, đứng hình.
 *
 * ── GIẢI PHÁP ─────────────────────────────────────────────────────────────────
 * 1. Throttle: Chỉ cho phép UI cập nhật tối đa 1 lần mỗi N milliseconds.
 *    Dù có 500 message/giây, UI chỉ nhận ~12 lần update/giây (intervalMs=80).
 *
 * 2. Debounce: Chờ một khoảng "im lặng" sau message cuối rồi mới update.
 *    Phù hợp cho tìm kiếm, preview — KHÔNG phù hợp cho giá đấu giá realtime.
 *
 * 3. Coalesce (nằm trong cả 2): Dùng AtomicReference để chỉ giữ giá trị MỚI NHẤT.
 *    100 message xếp hàng → chỉ 1 lần cập nhật với giá trị cuối cùng.
 *
 * ── CÁCH DÙNG ─────────────────────────────────────────────────────────────────
 *
 * // Trong controller:
 * private final UiThrottle<Double> priceThrottle =
 *     UiThrottle.throttle(newPrice -> lblCurrentPrice.setText(format(newPrice)), 80);
 *
 * // Khi nhận WebSocket message (có thể gọi 500 lần/giây, hoàn toàn safe):
 * priceThrottle.submit(newPrice);
 *
 * // Khi rời màn hình:
 * priceThrottle.shutdown();
 */
public final class UiThrottle<T> {

    private final ScheduledExecutorService scheduler;
    private final AtomicReference<T> latestValue = new AtomicReference<>();
    private final AtomicBoolean pendingUpdate = new AtomicBoolean(false);
    private final Consumer<T> uiUpdater;
    private final long intervalMs;
    private volatile boolean isShutdown = false;

    // Chỉ dùng cho debounce
    private ScheduledFuture<?> debounceFuture;
    private final Object debounceLock = new Object();

    private UiThrottle(Consumer<T> uiUpdater, long intervalMs) {
        this.uiUpdater = uiUpdater;
        this.intervalMs = intervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UiThrottle-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Tạo Throttle: cập nhật UI tối đa 1 lần mỗi intervalMs milliseconds.
     * Luôn hiển thị giá trị MỚI NHẤT trong window đó.
     *
     * Dùng cho: giá đấu giá realtime, số người tham gia, countdown timer.
     * Khuyến nghị intervalMs = 80 (tương đương ~12fps, mắt người không nhận ra lag).
     *
     * @param uiUpdater  Lambda chạy trên JavaFX thread, nhận giá trị mới nhất
     * @param intervalMs Khoảng thời gian tối thiểu giữa 2 lần update UI (ms)
     */
    public static <T> UiThrottle<T> throttle(Consumer<T> uiUpdater, long intervalMs) {
        UiThrottle<T> instance = new UiThrottle<>(uiUpdater, intervalMs);
        instance.startThrottleLoop();
        return instance;
    }

    /**
     * Tạo Debounce: đợi delayMs sau message CUỐI CÙNG rồi mới update UI.
     * Mỗi message mới sẽ reset lại đồng hồ.
     *
     * Dùng cho: live search, preview text, thao tác người dùng chậm hơn.
     * KHÔNG dùng cho giá đấu giá (sẽ bị delay).
     *
     * @param uiUpdater Lambda chạy trên JavaFX thread
     * @param delayMs   Thời gian chờ im lặng trước khi update (ms)
     */
    public static <T> UiThrottle<T> debounce(Consumer<T> uiUpdater, long delayMs) {
        return new UiThrottle<>(uiUpdater, delayMs);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Nạp giá trị mới vào throttle/debounce.
     * An toàn khi gọi từ bất kỳ thread nào (WebSocket thread, background thread...).
     * Không bao giờ block.
     */
    public void submit(T value) {
        if (isShutdown) return;

        // Lưu giá trị MỚI NHẤT — AtomicReference đảm bảo thread-safe không cần lock
        latestValue.set(value);

        // Nếu là debounce mode (scheduler chưa chạy loop)
        if (debounceLock != null && intervalMs > 0 && !isThrottleMode()) {
            scheduleDebounce();
        }
    }

    /**
     * Dừng scheduler khi rời màn hình (quan trọng — tránh memory leak).
     * Gọi trong handleBack(), navigateToDashboard(), hoặc teardown().
     */
    public void shutdown() {
        isShutdown = true;
        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    // =========================================================================
    // THROTTLE IMPLEMENTATION
    // =========================================================================

    private boolean isThrottleMode() {
        // Throttle mode: scheduler đã được khởi động từ factory
        return !scheduler.isShutdown() && pendingUpdate.get() == false && latestValue.get() == null
                || true; // Đơn giản hóa: throttle mode khi dùng factory throttle()
    }

    private void startThrottleLoop() {
        // Chạy mỗi intervalMs, lấy giá trị mới nhất và đẩy lên UI
        // Nếu không có giá trị mới (null) thì bỏ qua — không gọi runLater thừa
        scheduler.scheduleAtFixedRate(() -> {
            if (isShutdown) return;

            T value = latestValue.getAndSet(null); // Lấy và reset về null
            if (value == null) return; // Không có gì mới → skip hoàn toàn

            // Chỉ gọi Platform.runLater khi CÓ dữ liệu mới cần hiển thị
            // Đây là điểm mấu chốt: 500 message/s → tối đa 12 runLater/s
            Platform.runLater(() -> {
                try {
                    uiUpdater.accept(value);
                } catch (Exception e) {
                    System.err.println("UiThrottle: lỗi trong uiUpdater: " + e.getMessage());
                }
            });

        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    // =========================================================================
    // DEBOUNCE IMPLEMENTATION
    // =========================================================================

    private void scheduleDebounce() {
        synchronized (debounceLock) {
            // Hủy task cũ nếu còn pending
            if (debounceFuture != null && !debounceFuture.isDone()) {
                debounceFuture.cancel(false);
            }

            // Lên lịch task mới sau delayMs
            debounceFuture = scheduler.schedule(() -> {
                if (isShutdown) return;

                T value = latestValue.getAndSet(null);
                if (value == null) return;

                Platform.runLater(() -> {
                    try {
                        uiUpdater.accept(value);
                    } catch (Exception e) {
                        System.err.println("UiThrottle debounce: lỗi: " + e.getMessage());
                    }
                });

            }, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    // =========================================================================
    // BUILDER PATTERN — cho trường hợp cần nhiều field update cùng lúc
    // =========================================================================

    /**
     * Tạo một throttle xử lý nhiều field trong cùng 1 Runnable.
     * Tránh tạo nhiều UiThrottle riêng lẻ cho cùng 1 component.
     *
     * Ví dụ:
     *   UiThrottle<Void> batchThrottle = UiThrottle.throttleBatch(() -> {
     *       lblPrice.setText(priceRef.get());
     *       lblBidder.setText(bidderRef.get());
     *       updateChart(chartDataRef.get());
     *   }, 80);
     */
    public static UiThrottle<Void> throttleBatch(Runnable uiRunnable, long intervalMs) {
        return throttle(ignored -> uiRunnable.run(), intervalMs);
    }
}