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

    // FACTORY METHODS

    /**
     * Tạo Throttle: cập nhật UI tối đa 1 lần mỗi intervalMs milliseconds.
     * Luôn hiển thị giá trị MỚI NHẤT trong window đó.
     */
    public static <T> UiThrottle<T> throttle(Consumer<T> uiUpdater, long intervalMs) {
        UiThrottle<T> instance = new UiThrottle<>(uiUpdater, intervalMs);
        instance.startThrottleLoop();
        return instance;
    }


    // PUBLIC API

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

    // THROTTLE IMPLEMENTATION

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
            Platform.runLater(() -> {
                try {
                    uiUpdater.accept(value);
                } catch (Exception e) {
                    System.err.println("UiThrottle: lỗi trong uiUpdater: " + e.getMessage());
                }
            });

        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    // DEBOUNCE IMPLEMENTATION

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
}